(ns core-api.context)

(require '[clojure.core.async :as async :refer [chan >!! <!!]])

;; Outside "Context": Defines what input places, output places, and events exist
;; This would be custom for every program (or at least for every hardware architecture)

(def in-n-out-place (atom 0))
(def in-n-out-place2 (atom ""))
(def timeout-chan (chan))

;; Input Context
(def stdin-place (atom ""))
(def stdin {:place-tag :raw :place-name :stdin})
(def custom-in {:place-tag :raw :place-name :custom-in})
(def custom-in2 {:place-tag :raw :place-name :custom-in2})
(def in-places-map
  {:stdin stdin-place
   :custom-in in-n-out-place
   :custom-in2 in-n-out-place2})

;; Output Context
(def stdout {:place-tag :raw :place-name :stdout})  ;; adding the :raw tag so it can be used as a generic place
(def custom-out {:place-tag :raw :place-name :custom-out})
(def custom-out2 {:place-tag :raw :place-name :custom-out2})
(def timeout {:place-tag :raw :place-name :timeout})
(def exit {:place-tag :raw :place-name :exit})
(def out-places-map
  {:stdout println
   :custom-out (fn [val] (reset! in-n-out-place val))
   :custom-out2 (fn [val] (reset! in-n-out-place2 val))
   :timeout (fn [val] (when (> val 0) (future (Thread/sleep val)
                                              (>!! timeout-chan '()))))
   :exit (fn [val] (System/exit val))})

;; Events Context

(defn on-start!
  [event-handler]
  (event-handler))

(defn subscribe-to-timeout!
  [event-handler]
  (future (<!! timeout-chan)
          (event-handler)
          (subscribe-to-timeout!)))

(defn subscribe-to-std-in!
  [event-handler]
  (future (let [input (read-line)]
              (swap! stdin-place (constantly input)))
          (event-handler)
          (subscribe-to-std-in! event-handler)))

(def events
  {:start           on-start!
   :timeout-event   subscribe-to-timeout!
   :read-line-event subscribe-to-std-in!})

(def context
  {:in-places-map   in-places-map
   :out-places-map  out-places-map
   :events          events})
