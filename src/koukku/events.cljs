(ns koukku.events
  "Event based app state with React reducer hook.

  Call `(with-state {:initial-state ...} root-component)` to initialize.
  The `root-component` will be called with the current app state.

  To create event dispatch callbacks call `e!` with an `Event` instance
  constructor and arguments. It will return a callback that will construct
  and process the event with any extra args passed along.

  To get the dispatcher function, that can be directly called with
  an event instance, use `use-dispatch`.

  Events must implement the `process-event` method which receives
  the event and the current app state and return the new app state.

  Events may return effects along with the new state by using `fx`
  function. An effect is a record that implements `Effect` protocol
  and the `process-effect` method. Effects are run after the next
  render using a React useEffect hook. Effects can side effect
  and use `e!` to get more callbacks to send events asynchronously."
  (:require react
            [koukku.html :as h]
            [koukku.core :as k]))

(defprotocol Event
  (process-event [this state]
    "Process this event against current state and return new state."))

(defprotocol Effect
  (process-effect [this]
    "Process this side-effect. May use additional event callbacks with `e!`.
Return value is ignored."))

(deftype EffectReturn [state effects])

(defn fx
  "Creates an effect to be returned from a `process-event` invocation.
  Takes the updated `state` and one or more effects to be
  applied. An effect is an instance of `Effect`.

  Effects are run after the state has been updated to its
  new value.
  "
  [state & effects]
  (EffectReturn. state effects))

(defonce dispatch-context (react/createContext nil))
(defonce dispatch-provider (aget dispatch-context "Provider"))

(defn use-dispatch
  "Returns the dispatcher inside the component tree of `with-state`."
  []
  (react/useContext dispatch-context))

;; Dispatch used inside process-effect
(def ^{:private true
       :dynamic true} *dispatch* nil)

(defn e! [& event-constructor-and-args]
  (let [create-callback
        (fn [dispatch!]
          (fn [& more-args]
            (let [evt (apply (first event-constructor-and-args)
                             (concat (drop 1 event-constructor-and-args)
                                     more-args))]
              (assert (satisfies? Event evt) "constructor must return an Event instance")
              (dispatch! evt))))]
    (if-let [dispatch! *dispatch*]
      ;; Inside process-effect
      (create-callback dispatch!)

      ;; Inside component render
      (let [dispatch! (use-dispatch)]
        ;; PENDING: useCallback memoing doesn't seem to have benefit here
        (create-callback dispatch!)
        #_(react/useCallback (create-callback dispatch!)
                           (into-array event-constructor-and-args))))))

(defn- dispatch-event [set-effects! state event]
  (let [ret (process-event event state)]
    (if (instance? EffectReturn ret)
      (do (set-effects! (vec (.-effects ret)))
          (.-state ret))
      ret)))

(defn- process-effects [dispatch! effects]
  (binding [*dispatch* dispatch!]
    (doseq [effect effects]
      (assert (satisfies? Effect effect) "effect must be an Effect instance")
      (process-effect effect))))

(defn with-state
  "Main entry point for event system."
  [{:keys [initial-state] :as _opts} root-component]
  (let [[effects-to-run set-effects!] (k/use-state nil)
        [state dispatch!] (k/use-reducer (fn [state event]
                                           (dispatch-event set-effects! state event))
                                         initial-state)]
    (k/use-effect #(process-effects dispatch! effects-to-run) true effects-to-run)
    (react/createElement dispatch-provider #js {:value dispatch!}
                         (h/html
                          [root-component state]))))
