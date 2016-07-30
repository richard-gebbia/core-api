(ns core-api.api)

(require '[clojure.core.async :as async :refer [chan poll!]])

;; ===============
;; The Core API
;; ===============

;; Private methods
(defmulti #^{:private true} map-in-place (fn [context place] (:place-tag place)))
(defmethod #^{:private true} map-in-place :raw
  [context place]
  (let [in-places-map (:in-places-map context) 
        place-name (:place-name place)
        place-atom (place-name in-places-map)] 
    (identity [place-name @place-atom])))

(defn- map-in-places
  "Gets the current values of the data in the specified places"
  [context in-places]
  (into {} (map 
    (partial map-in-place context)
    in-places)))

(defmethod #^{:private true} map-in-place :connect
  [context place]
  (let [in-places-map     (:in-places-map context)
        place-name        (:place-name place)
        connect-fn        (:connect-fn place)
        dependent-places  (:dependent-places place)
        connect-fn-input  (map-in-places context dependent-places)
        place-value       (connect-fn connect-fn-input)]
    (identity [place-name place-value])))

(defmulti #^{:private true} map-out-place (fn [context place] (:place-tag place)))
(defmethod #^{:private true} map-out-place :raw 
  [context place]
  ;; if it's not a spread place, 
  ;; then the place-name is an actual key to a place in 
  ;; "out-places-map"
  (let [out-places-map (:out-places-map context)]
    (fn [value] ((out-places-map (:place-name place)) value))))

(defn- map-out-places
  [context places-to-values]
  (into () 
    (map 
      (fn [[place value]]
        ((map-out-place context place) value))
      places-to-values)))

(defmethod #^{:private true} map-out-place :spread
  [context place]
  ;; if the place is a spread place,
  ;; then instead of place-name, we have a spread-fn
  ;; that takes the value and returns a map place tags to values
  ;; (those place tags can themselves be spreads)
  (let [spread-fn (:spread-fn place)]
    (fn [value] (map-out-places context (spread-fn value)))))

;; Public methods

(defn endless! []
  "Enlessly spin-locks. Effectively prevents the program from terminating
   without an explicit call to System/exit."
  (let [poll-chan (chan)]
    (loop []
      (do (Thread/sleep 1)
          (poll! poll-chan)
          (recur)))))

(defn on-event
  "The major function of the Core API. You can read a call to this as:
   when {event} happens, grab the values at {in-places} provided by {context},
   perform {xform}, and move the data that {xform} mapped to their respective
   out-places provided by {context}.

   context - a map containing events, in-places (places to read from), and out-places (places to write to)
   event - the name of the event to subscribe to in {context}'s map from event names to events
   in-places - a vector of the names of in-places to read from when the event occurs
   xform - a function that takes a map of in-place names to in-place values and
           returns a map of out-place names to out-place values;
           the values in the parameter map are 'grabbed' at the time of the event;
           the values in the returned map are 'sent' to their respective out-places

  Calling this function maintains a continuous subscription, meaning that the
  program will trigger {xform} EVERY TIME {event} happens, forever."
  [context event in-places xform]
  (let [events (:events context)
        subscribe (events event)]
    (subscribe (fn []
                 (let [in-places-to-values (map-in-places context in-places)]
                   (map-out-places context (xform in-places-to-values)))))))

(defn add-context
  "Partially applies on-event with its first parameter, since the context should
   be the same for every call to on-event within the same program."
  [context]
  (partial on-event context))
