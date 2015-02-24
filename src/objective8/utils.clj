(ns objective8.utils
  (:require [clj-time.format :as time-format]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [objective8.config :as config]))

(def host-url
  (str "http://" (config/get-var "BASE_URI" "localhost:8080")))

;;TIME FORMATTING

(defn string->date-time [date-string]
  (time-format/parse (time-format/formatters :year-month-day) date-string))

(defn date-time->iso-time-string [date-time]
  (str date-time))

(defn time-string->date-time [time-string]
  (time-format/parse (time-format/formatters :date-time) time-string))

(def pretty-date (time-format/formatter "dd-MM-yyyy"))

(def pretty-date-time (time-format/formatter "dd-MM-yyyy HH:mm"))

(defn time-string->pretty-date [time-string]
  (time-format/unparse pretty-date (time-string->date-time time-string)))

(defn date-time->pretty-date [date-time]
  (time-format/unparse pretty-date date-time))

(defn iso-time-string->pretty-time [iso-time-string]
  (time-format/unparse pretty-date-time (time-string->date-time iso-time-string)))

(defn- regex-checker
  [fragment-regex]
    (fn [fragment] (when fragment (re-matches fragment-regex fragment))))

(defn safen-url [target]
  (or ((regex-checker #"/objectives/\d+") target) 
      ((regex-checker #"/objectives/\d+/questions") target)  
      ((regex-checker #"/objectives/\d+/questions/\d+") target)))

;;DISABLE CSRF for tests

(defn anti-forgery-hook [handler]
  (let [handler-with-anti-forgery (wrap-anti-forgery handler)]
    (fn [request] (if config/enable-csrf
                    (handler-with-anti-forgery request)
                    (handler request)))))
