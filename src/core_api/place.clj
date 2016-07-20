(ns core-api.place)

;; Interface with Places

;; TODO(Richard): make a connect function that does the inverse for input places

(defn spread 
    [spread-fn]
    {:place-tag :spread :place-name spread-fn})
