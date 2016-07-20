(ns core-api.core
    (:gen-class))

(require '[core-api.context :as context])
(require '[core-api.api     :as api])

;; Test programs

(defn write-hello [_]
    {context/stdout "hola!"})

(defn write-input [place-map]
    {context/stdout (:stdin place-map)})

(def register-recurring (api/add-context context/context))

(defn -main [& args]
    ;; (on-recurring-event :read-line-event [] write-hello)
    (register-recurring :read-line-event [:stdin] write-input))

