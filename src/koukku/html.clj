(ns koukku.html
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [koukku.html.markdown :as md]))

(if (System/getenv "KOUKKU_DEBUG")
  (defn log [& things]
    (with-open [out (io/writer (io/file "koukku.debug") :append true)]
      (.write out
              (str (str/join " " things) "\n"))))
  (defn log [& things]))

(defn to-camel-case [kw]
  (str/replace (name kw)
               #"-\w"
               (fn [m]
                 (str/upper-case (subs m 1)))))

(def special-keys {:class "className"})

(defn ->js
  "Compile time js conversion.

  Conversion rules:
  maps => (js-obj \"key1\" val1 ...)
  keywords (as value) => name string
  keywords (as key) => camelCased name string (or map special keys like :class => \"className\")
  vector => (array val1 ...)

  Other values are passed as is."
  [x]
  (cond
    (map? x)
    `(cljs.core/js-obj ~@(mapcat
                          (fn [[k v]]
                            [(cond
                               (keyword? k) (or (special-keys k)
                                                (to-camel-case k))
                               (or (string? k)
                                   (number? k)) k
                               ;; some weird key, try runtime conversion
                               :else `(koukku.html/->js k))
                             (->js v)]) x))

    (vector? x)
    `(cljs.core/array ~@(map ->js x))

    (keyword? x)
    (name x)

    :else x))

(defn element-class-names [elt]
  (map second (re-seq #"\.([^.#]+)" (name elt))))

(defn element-name [elt]
  (second (re-find #"^([^.#]+)" (name elt))))

(defn element-id [elt]
  (second (re-find #"#([^.]+)" (name elt))))

(declare compile-html)

(defn props-and-children [body]
  (let [has-props? (map? (second body))
        props (if has-props?
                (second body)
                nil)
        children (drop (if has-props? 2 1) body)]
    [props children]))

(defn compile-children [children]
  (map compile-html children))

(defn get-key [body]
  (-> body meta :key))

(defn compile-html-element
  "Compile HTML markup element, like [:div.someclass \"content\"]."
  [body]
  (let [element-kw (first body)
        element (element-name element-kw)
        class-names (element-class-names element-kw)
        id (element-id element-kw)
        [props children] (props-and-children body)
        props (merge (when-let [k (get-key body)]
                       {:key k})
                     props
                     (when (seq class-names)
                       {:className (str/join " " class-names)})
                     (when id
                       {:id id}))]
    (log "HTML Element:" element "with props:" props "and" (count children) "children")
    `(koukku.html/->elt
      ~(if (keyword? element)
         (name element)
         element)
      ~(->js props)

      ~@(compile-children children))))

(defn compile-component [body]
  (let [component-fn (first body)
        args (subvec body 1)
        key (get-key body)]
    (log "compile-component, component-fn=" component-fn ", args=" args ", key=" key)
    `(koukku.html/->elt
      (koukku.html/component-fn-host ~(str component-fn) ~component-fn)
      ;; PENDING: check element is valid? (like symbol)
      (cljs.core/js-obj ~@(when key ["key" key])
                        "args" ~args))))

(defn compile-fragment [body]
  (let [[props children] (props-and-children body)
        key (get-key body)]
    (log "Fragment with props: " props " and key " key)
    `(koukku.html/->fragment
      ~(->js (if key
               (merge {:key key} props)
               props))
      ~@(compile-children children))))

(defn compile-js-component [body]
  (let [key (get-key body)
        body (drop 1 body)
        component (first body)
        [props children] (props-and-children body)]
    (log "JS component, component=" component ", props=" props ", children="children ", key=" key)
    `(koukku.html/->elt
      (koukku.html/js-comp ~component)
      ~(->js (if key
               (merge {:key key} props)
               props))
      ~@(compile-children children))))

(defn compile-for
  "Compile special :koukku.html/for element."
  [[_ bindings body :as form]]
  (assert (vector? bindings) ":koukku.html/for bindings must be a vector")
  (assert (= 3 (count form)) ":koukku.html/for must have bindings and a single child form")
  `(into-array
    (for ~bindings
      ~(compile-html body))))

(defn compile-if
  "Compile special :koukku.html/if element."
  [[_ test then else :as form]]
  (assert (= 4 (count form)) ":koukku.html/if must have exactly 3 forms: test, then and else")
  `(if ~test
     ~(compile-html then)
     ~(compile-html else)))

(defn compile-when
  "Compile special :koukku.html/when element."
  [[_ test then :as form]]
  (assert (= 3 (count form)) ":koukku.html/when must have exactly 2 forms: test and then")
  `(when ~test
     ~(compile-html then)))

(defn compile-cond
  "Compile special :koukku.html/cond element."
  [[_ & clauses]]
  (assert (even? (count clauses)) ":koukku.html/cond must have even number of forms")
  `(cond
     ~@(mapcat (fn [[test expr]]
                 [test (compile-html expr)])
               (partition 2 clauses))))

(defn compile-md
  "Compile special :koukku.html/md element."
  [body]
  (let [[_props children] (props-and-children body)]
    ;; PENDING: props ignored for now, could be used to
    ;; customize generated elements
    (-> children md/compile-md compile-html)))

(def compile-special {:<> compile-fragment
                      :> compile-js-component
                      ::for compile-for
                      ::if compile-if
                      ::when compile-when
                      ::cond compile-cond
                      ::md compile-md})

(defn compile-html [body]
  (cond
    (vector? body)
    (cond
      ;; first element is special element
      (contains? compile-special (first body))
      ((compile-special (first body)) body)

      ;; first element is a keyword this is static HTML markup
      (keyword? (first body))
      (compile-html-element body)

      ;; otherwise treat first element as function component
      :else
      (compile-component body))

    ;; Some content: a static string or symbol reference
    ;; or a list that evaluates to children
    (or (string? body)
        (symbol? body)
        (list? body))
    body

    :else
    (throw (ex-info (str "Can't compile to HTML: " (pr-str body))
                    {:element body}))))

(defmacro html
  "Render hiccup HTML as React elements."
  [body]
  (compile-html body))
