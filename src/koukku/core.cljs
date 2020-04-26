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

(defn use-effect
  "Simple wrapper for react/useEffect. Takes effect fn
  and optional dependencies.

  If effect-fn returns a function, it is used as the cleanup."
  [effect-fn & dependencies]
  (react/useEffect (fn []
                     (let [ret (effect-fn)]
                       (if (fn? ret)
                         ret
                         (constantly nil))))
                   (into-array dependencies)))

(defn use-state
  "Simple wrapper for react/useState."
  [initial-state]
  (react/useState initial-state))

(defn use-reducer
  "Simple wrapper for react/useReducer.
  Wraps the reducer-fn in a function so any clojure `ifn?` implementing
  object with 2-arity can be used as reducer (like multimethods)."
  [reducer-fn initial-state]
  (react/useReducer (fn [state action]
                      (reducer-fn state action))
                    initial-state))
