(ns koukku.html
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(if (System/getenv "KOUKKU_DEBUG")
  (defn log [& things]
    (with-open [out (io/writer (io/file "koukku.debug") :append true)]
      (.write out
              (str (str/join " " things) "\n"))))
  (defn log [& things]))

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

(defn compile-html-element
  "Compile HTML markup element, like [:div.someclass \"content\"]."
  [body]
  (let [element-kw (first body)
        element (element-name element-kw)
        class-names (element-class-names element-kw)
        id (element-id element-kw)
        [props children] (props-and-children body)
        props (merge props
                     (when class-names
                       {:className (str/join " " class-names)})
                     (when id
                       {:id id}))]
    (log "HTML Element:" element "with props:" props "and" (count children) "children")
    `(koukku.html/->elt ~(if (keyword? element)
                           (name element)
                           element)
                        (koukku.html/->js ~props)
                        (koukku.html/gather-children
                         ~@(map compile-html children)))))

(defn compile-fragment [body]
  (let [[props children] (props-and-children body)]
    `(koukku.html/->elt react/Fragment
                        ~props
                        (koukku.html/gather-children
                         ~@(map compile-html children)))))

(defn compile-component [body]
  (let [component-fn (first body)
        args (subvec body 1)]
    (log "compile-component, component-fn=" component-fn ", args=" args)
    `(koukku.html/->elt
      koukku.html/component-fn-host
      ;; PENDING: check element is valid? (like symbol)
      (cljs.core/js-obj "component-fn" ~component-fn
                        "args" ~args)
      (cljs.core/array))))

(defn compile-html [body]
  (cond
    (vector? body)
    (cond
      ;; first element is :<>, this is a fragment
      (= :<> (first body))
      (compile-fragment body)

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
    (throw (ex-info "Can't compile to HTML" {:element body}))))

(defmacro html
  "Render hiccup HTML as React elements."
  [body]
  (compile-html body))
