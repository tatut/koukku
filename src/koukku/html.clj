(ns koukku.html
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(if (System/getenv "KOUKKU_DEBUG")
  (let [dbg (io/writer (io/file "koukku.debug"))]
    (defn log [& things]
      (.write dbg
              (str/join " " things))))
  (defn log [& things]))

(defn element-class-names [elt]
  (map second (re-seq #"\.([^.#]+)" (name elt))))

(defn element-name [elt]
  (second (re-find #"^([^.#]+)" (name elt))))

(defn element-id [elt]
  (second (re-find #"#([^.]+)" (name elt))))

(declare compile-html)

(defn compile-html-element
  "Compile HTML markup element, like [:div.someclass \"content\"]."
  [body]
  (let [element-kw (first body)
        element (element-name element-kw)
        class-names (element-class-names element-kw)
        id (element-id element-kw)
        has-props? (map? (second body))
        props (merge
               (if has-props?
                 ;; FIXME: what if props is like: (merge {:some 1} (when ..))
                 ;; this ONLY accepts map, so no dynamic props
                 (second body)
                 nil)
               (when class-names
                 {:className (str/join " " class-names)})
               (when id
                 {:id id}))
        children (drop (if has-props? 2 1) body)]
    (log "HTML Element:" element "with props:" props "and" (count children) "children")
    `(koukku.html/->elt ~(if (keyword? element)
                           (name element)
                           element)
                        (koukku.html/->js ~props)
                        (koukku.html/gather-children
                         ~@(map compile-html children)))))

(defn compile-html [body]
  (cond
    ;; Vector where the first element is a keyword
    ;; this is static HTML markup
    (and (vector? body)
         (keyword? (first body)))
    (compile-html-element body)

    ;; otherwise this is a call to a component
    (vector? body)
    (throw (ex-info "please implement" {:body body}))

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
