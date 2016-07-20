(ns core-api.time)

;; Interface with Time

(def t0 "Indicates the point in time of the start of the program" 0)

(def immediately "Indicates a time-span of 0" 0)

(def second-scale 
    "The amount of time a second is worth.
    Actual value is arbitrary but used to compute 
    the weight given to other slices of time.
    Please feel free to redefine this to something
    more or less precise to suit your needs."
    1000)

(defn seconds
    [num-seconds]
    (* second-scale num-seconds))

(defn milliseconds
    [num-ms]
    (/ 1000 (* second-scale num-ms)))

(defn after
    "Given a point in time, specifies another point in time 
    'time-span' time units after it."
    [time-span time-point]
    (+ time-point time-span))
