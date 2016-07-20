(ns core-api.context)

(require '[clojure.core.async :as async :refer [chan go >! <!!]])

;; Outside "Context": Defines what input places, output places, and events exist
;; This would be custom for every program (or at least for every hardware architecture)

;; Input Context
(def stdin-place (atom ""))
(def in-places-map
    {:stdin stdin-place})

;; Output Context
(def stdout {:place-tag :raw :place-name :stdout})  ;; adding the :raw tag so it can be used as a generic place
(def out-places-map
    {:stdout println})

;; Events Context
(defn subscribe-to-std-in!
    [event-handler in-places-map place-getter]
    ;; make a channel, then loop endlessly:
    ;; - make background thread that blocks on "read-line"
    ;; - once "read-line" returns, pipe its return value into the channel
    ;; - when the channel receives a value, 
    ;;      - swap that value into the "stdin-place" atom
    ;;      - then call the event-handler
    (let [in-chan (chan)]
        (loop [user-input ""]
            (recur (do
                (go (>! in-chan (read-line)))
                (let [input (<!! in-chan)]
                    (do (swap! stdin-place (constantly input))
                        (event-handler (place-getter in-places-map)))))))))

(def events
    {:read-line-event subscribe-to-std-in!})

(def context
    {:in-places-map in-places-map
     :out-places-map out-places-map
     :events events})
