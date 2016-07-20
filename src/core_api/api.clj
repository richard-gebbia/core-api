(ns core-api.api)

;; ===============
;; The Core API
;; ===============

;; Private methods
(defn- get-in-place-values
    "Gets the current values of the data in the specified places"
    ;; right now, we're treating everything as raw atoms,
    ;; but later, I should change this to work with "connect"
    [context in-place-keys]
    (let [in-places-map (:in-places-map context)]
        (into {} 
            (map 
                (fn [[key atom]] (identity [key @atom])) 
                (select-keys in-places-map in-place-keys)))))

(defmulti #^{:private true} map-place (fn [context place] (:place-tag place)))
(defmethod #^{:private true} map-place :raw 
    [context place]
    ;; if it's not a spread place, 
    ;; then the place-name is an actual key to a place in 
    ;; "out-places-map"
    (let [out-places-map (:out-places-map context)]
        (fn [value] ((out-places-map (:place-name place)) value))))

(defn- map-places
    [context places-to-values]
    (into () 
        (map 
            (fn [[place value]]
                ((map-place context place) value))
            places-to-values)))

(defmethod #^{:private true} map-place :spread
    [context place]
    ;; if the place is a spread place,
    ;; then the place-name is actually a function
    ;; that takes the value and returns a map place tags to values
    ;; (those place tags can themselves be spreads)
    (fn [value] (map-places context ((:place-name place) value))))

;; Public methods

;; TODO(Richard): make a plain on-event function hooks a function for a one-shot event

(defn on-recurring-event
    [context event in-places xform]
    (let [events (:events context)]
        ((events event) 
            (fn [in-places-to-values] (map-places context (xform in-places-to-values)))
            in-places
            (partial get-in-place-values context))))

(defn add-context
    [context]
    (partial on-recurring-event context))