(ns d-cent.utils
  (:require [d-cent.config :as config]))

(def host-url
  (str "http://"
    (config/get-var "BASE_URI" "localhost")
     ":"
    (config/get-var "PORT" "8080")
  ))
