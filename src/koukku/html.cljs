(ns koukku.html
  (:require-macros [koukku.html])
  (:require react
            [goog.object :as gobj]))

(defn ->js [x]
  (cond
    (map? x)
    (reduce (fn [acc [key val]]
              (aset acc
                    (if (keyword? key)
                      (name key)
                      key)
                    (->js val))
              acc)
            #js {}
            x)

    (vector? x)
    (reduce (fn [acc val]
              (.push acc val)
              acc)
            #js []
            x)

    :else
    x))

(defn gather-children [& children]
  (reduce
   (fn [acc child]
     (.push acc child)
     acc)
   #js []
   (mapcat #(if (seq? %) % [%]) children)))

(defn component-fn-host [comp-name]
  (let [host (fn [props _children]
               (let [comp-fn (aget props "component-fn")
                     args (aget props "args")]
                 (apply comp-fn args)))]
    (set! (.-displayName host) comp-name)
    host))

(defn ->elt [element props & children]
  (apply react/createElement
         element
         props
         children))

(defn ->fragment [props & children]
  (apply react/createElement
         react/Fragment
         props
         children))

(defn js-comp
  "Return JS component usable with react/createElement.
  If comp is js object containing \"default\" key, it is
  a required module.
  Otherwise the component is returned as is."
  [comp]
  (if (and (object? comp)
           (gobj/containsKey comp "default"))
    (gobj/get comp "default")
    comp))
