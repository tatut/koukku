(ns koukku.demo
  (:require react
            [koukku.core :as k]
            [koukku.html :as h]))

(defn list-item [on-delete {:keys [name amount]}]
  (h/html
   [:li (if (zero? amount)
          (str "we have no " name "s")
          (str  amount " " name "s"))
    [:button {:onClick on-delete}
     "delete"]]))

(def initial-fruit-inventory [{:name "banana" :amount 0}
                              {:name "apple" :amount 7}
                              {:name "kiwi" :amount 2}])

(defn my-component []
  (let [[fruits set-fruits!] (react/useState initial-fruit-inventory)
        remove-fruit! (fn [f]
                        (set-fruits! (filterv #(not= f %) fruits)))]
    (h/html
     [:div.foo {:style {:border "solid 1px black"}}
      (if (seq fruits)
        (h/html
         [:div
          [:b "here's the fruits we have:"]
          [:ul
           (for [f fruits]
             (list-item #(remove-fruit! f) f))]])
        (h/html
         [:div "oh noes, we have no fruits"]))])))

(defn main []
  (k/main my-component "app"))

(main)
