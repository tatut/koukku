(ns calculator.events
  (:require [koukku.events :as ev]))

(defrecord InputNumber [num])
(defrecord Op [op])
(defrecord Clear [])

(defn operate [op n1 n2]
  ((case op
      "*" *
      "/" /
      "-" -
      "+" +) n1 n2))

(defn handle-op [op {:keys [input-number result store-op] :as state}]
  (cond
    (nil? result)
    {:result input-number
     :store-op (if (= op "=") nil op)
     :input-number 0}

    (and store-op (= op "="))
    {:result (operate store-op result input-number)
     :input-number 0}

    (= op "=")
    state

    :else
    {:result (operate (or store-op op) result input-number)
     :store-op (if store-op op nil)
     :input-number 0}))

(defrecord BeastEffect []
  ev/Effect
  (process-effect [_]
    (js/alert "Oh no, that's a bad number!")
    (js/setTimeout (ev/e! ->Clear) 2000)))

(extend-protocol ev/Event
  InputNumber
  (process-event [{num :num} state]
    (update state :input-number #(+ (* 10 %) num)))

  Op
  (process-event [{op :op} state]
    (let [new-state (handle-op op state)]
      (if (= 666 (:result new-state))
        ;; unacceptable!
        (ev/fx new-state
               (->BeastEffect))

        ;; carry on calculating
        new-state)))

  Clear
  (process-event [_ _]
    {:input-number 0}))
