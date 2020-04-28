(ns showcase.core
  "Various little tests for koukku... nothing interesting"
  (:require [koukku.core :as k]
            [koukku.html :as h]))

(defn markdown [{:keys [name description]}]
  (h/html
   [::h/md
    "
## This is my section
And here I have a list of my info:
- **name**: " name "
- **description**: " description "


"]))

(defn showcase-root []
  (h/html
   [:<>
    [:h1 "markdown test"]
    [markdown {:name "Koukku"
               :description "a React library"}]]))

(defn main []
  (k/main showcase-root "app"))

(main)
