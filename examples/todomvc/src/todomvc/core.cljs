(ns todomvc.core
  "The traditional TodoMVC with Material UI"
  (:require react
            [koukku.core :as k]
            [koukku.html :as h]
            ["@material-ui/core/List" :as mui-List]
            ["@material-ui/core/ListItem" :as mui-ListItem]
            ["@material-ui/core/ListItemText" :as mui-ListItemText]
            ["@material-ui/core/ListItemIcon" :as mui-ListItemIcon]
            ["@material-ui/core/Checkbox" :as mui-Checkbox]
            ["@material-ui/core/TextField" :as mui-TextField]
            [clojure.string :as str]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event handling
;;
;; Create a multimethod that takes app state and event
;; and returns new app state.
;;
;; The event multimethod is then used as reducer.

(defmulti event (fn [_app action] (:event action)))

(defn- update-event [app id new-data]
  (update app :todos
          (fn [todos]
            (mapv #(if (= id (:id %))
                     (merge % new-data)
                     %)
                  todos))))

(defmethod event :mark-complete [app {id :id}]
  (update-event app id {:complete? true}))

(defmethod event :mark-incomplete [app {id :id}]
  (update-event app id {:complete? false}))

(defmethod event :new-todo [app {description :description}]
  (let [id (:new-todo-id app)]
    (-> app
        (update :new-todo-id inc)
        (update :todos conj {:id id
                             :description description
                             :complete? false}))))

(defmethod event :default [app event]
  (js/console.error "Unimplemented event: " (pr-str event))
  app)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initial app state containing some todos

(def initial-state {:todos [{:id 0
                             :description "make new React wrapper"
                             :complete? false}
                            {:id 1
                             :description "create todomvc example"
                             :complete? true}]
                    :new-todo-id 2})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The actual UI part

(defn todo-item [dispatch! {:keys [id description complete?]}]
  (h/html
   [:> mui-ListItem {:dense true :button true
                     :onClick #(dispatch! {:event (if complete?
                                                    :mark-incomplete
                                                    :mark-complete)
                                           :id id})}
    [:> mui-ListItemIcon
     [:> mui-Checkbox {:checked complete?}]]
    [:> mui-ListItemText {:primary description
                          :style {:textDecoration (if complete?
                                                    "line-through"
                                                    "none")}}]]))
(defn todo-list [dispatch! todos]
  (h/html
   [:> mui-List {:dense true}
    [::h/for [{id :id :as todo} todos]
     ^{:key id}
     [todo-item dispatch! todo]]]))

(defn todo-summary [todos]
  (h/html
   [:div (count (filter :complete? todos)) " out of " (count todos) " tasks complete"]))

(defn todo-form [dispatch!]
  (let [[text set-text!] (react/useState "")]
    (h/html
     [:> mui-TextField {:placeholder "What needs to be done?"
                        :value text
                        :onChange #(-> % .-target .-value set-text!)
                        :onKeyPress #(when (and (= "Enter" (.-key %))
                                                (not (str/blank? text)))
                                       (dispatch! {:event :new-todo
                                                   :description text}))}])))
(defn todo-app []
  (let [[{:keys [todos]} dispatch!] (react/useReducer (fn [app action]
                                                        (event app action))

                                                      initial-state)]

    (h/html
     [:div "Here's the todos:"

      [todo-list dispatch! todos]
      [todo-form dispatch!]
      [todo-summary todos]])))

;;;;;;;;;;;;;;;;;;;;;;;;;
;; The main entrypoint

(defn main []
  (k/main todo-app "app"))

(main)
