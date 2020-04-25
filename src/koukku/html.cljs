(ns koukku.html
  (:require-macros [koukku.html])
  (:require react))

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

(defn ->elt [element props children]
  (react/createElement element
                       props
                       children))
