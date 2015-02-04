(ns d-cent.utils
  (:require [clj-time.format :as time-format]
            [d-cent.config :as config]))

(def host-url
  (str "http://"
    (config/get-var "BASE_URI" "localhost")
     ":"
    (config/get-var "PORT" "8080")
  ))


;;TIME FORMATTING

(defn string->date-time [date-string]
  (time-format/parse (time-format/formatters :year-month-day) date-string))

(defn time-string->date-time [time-string]
  (time-format/parse (time-format/formatters :date-time) time-string))

(def pretty-date (time-format/formatter "dd-MM-yyyy"))

(defn time-string->pretty-date [time-string]
  (time-format/unparse pretty-date (time-string->date-time time-string)))

(defn date-time->pretty-date [date-time]
  (time-format/unparse pretty-date date-time))
