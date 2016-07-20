(ns core-api.time)

;; Interface with Time

(def t0 "Indicates the point in time of the start of the program" 0)

(def immediately "Indicates a time-span of 0" 0)

(defn after
    "Given a point in time, specifies another point in time 
    'time-span' time units after it."
    [time-span time-point]
    (+ time-point time-span))
