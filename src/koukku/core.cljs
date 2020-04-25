(ns koukku.core
  "Koukku the hook based React library"
  (:require react
            react-dom))

(defn- get-or-create-element [id]
  (let [elt (js/document.getElementById id)]
    (or elt
        (let [elt (js/document.createElement "div")]
          (js/document.body.appendChild elt)
          elt))))

(defn main
  "Main entrypoint: renders component to page."
  [component-fn element-or-id]
  (let [elt (if (string? element-or-id)
              (get-or-create-element element-or-id)
              element-or-id)]
    (react-dom/render (react/createElement component-fn nil) elt)))
