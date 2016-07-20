(ns core-api.place)

;; Interface with Places

(defn connect
    "Allows the user to 'connect' some places by providing a function that 
     takes a map from the 'connected' places to values and generates a single value.
     The value that this function returns is, itself, considered a single place,
     despite being representative of more than one place. Essentially, this
     function allows the user to compose input places."
    [dependent-places connect-fn]
    (identity 
        {:place-tag         :connect
         :dependent-places  dependent-places
         :connect-fn        connect-fn}))

(defn spread 
    "Allows the user to 'spread' some places by providing a function that takes
     a signle value and generates a map from the 'spread' places to values.
     The value that this function returns is, itself, considered a single place,
     despite being representative of more than one place. Essentially, this
     function allows the user to compose output places."
    [spread-fn]
    (identity
        {:place-tag :spread
         :place-name spread-fn})
