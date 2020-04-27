(ns calculator.core
  (:require [koukku.core :as k]
            [koukku.html :as h]
            [koukku.events :refer [e! with-state]]
            [calculator.events :as ev]))

(def initial-calculator-state {:input-number 0})

(defn number-button [n]
  (h/html
   [:button {:on-click (e! ev/->InputNumber n)}
    (str n)]))

(defn op-button [op]
  (h/html
   [:button {:on-click (e! ev/->InputNumber op)}
    op]))

(defn calculator [{:keys [input-number result] :as state}]
  (h/html
   [:<>
    [:table ;; A good old fashioned table
     [:tbody
      [:tr.result
       [:td {:col-span 4
             :style {:text-align :right}}
        (if (and result (zero? input-number))
          (str result)
          (str input-number))]]
      [::h/for [row [[7 8 9 nil]
                     [4 5 6 "*"]
                     [1 2 3 "/"]
                     [0 "+" "-" "="]]]
       ^{:key (first row)}
       [:tr.buttons
        [::h/for [btn row]
         ^{:key btn}
         [:td.button
          [::h/cond
           (nil? btn) ""
           (number? btn) [number-button btn]
           :else [op-button btn]]]]]]]]
    (pr-str state)]))

(defn calculator-root []
  (with-state {:initial-state initial-calculator-state}
    calculator))

(defn main []
  (k/main calculator-root "app"))

(main)
