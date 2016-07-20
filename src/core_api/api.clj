(ns core-api.api)

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
    (let [in-places-map (:in-places-map context)
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

;; TODO(Richard): make a plain on-event function hooks a function for a one-shot event

(defn on-recurring-event
    [context event in-places xform]
    (let [events (:events context)
          subscribe (events event)]
        (subscribe
            (fn [in-places-to-values] (map-out-places context (xform in-places-to-values)))
            in-places
            (partial map-in-places context))))

(defn add-context
    [context]
    (partial on-recurring-event context))