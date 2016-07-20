(ns core-api.core
    (:gen-class))

(require '[core-api.context :as context])
(require '[core-api.api     :as api])

;; Test programs

(defn write-hello [_]
    {context/stdout "hola!"})

(defn write-input [place-map]
    {context/stdout (:stdin place-map)})

(defn write-inc-place [place-map]
    {context/custom-out (inc (:custom-in place-map))
     context/stdout     (:custom-in place-map)})

(defn write-input-until-quit [place-map]
    (let [user-input (:stdin place-map)]
        (if (= user-input "quit")
            {context/exit 0}
            {context/stdout user-input})))

(def register-recurring (api/add-context context/context))

(defn -main [& args]
    ;; (on-recurring-event :read-line-event [] write-hello)
    ;; (register-recurring :read-line-event [context/stdin] write-input))
    ;; (register-recurring :read-line-event [context/custom-in] write-inc-place))
    (register-recurring :read-line-event [context/stdin] write-input-until-quit))

