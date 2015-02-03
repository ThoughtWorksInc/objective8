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

(defn string->time-stamp [date-string]
  (time-format/parse (time-format/formatters :year-month-day) date-string))

(defn time-string->time-stamp [time-string]
  (time-format/parse (time-format/formatters :date-time) time-string))

(defn time-stamp->string [time-stamp]
  (time-format/unparse (time-format/formatters :year-month-day) time-stamp))

(def pretty-date (time-format/formatter "dd-MM-yyyy"))

(defn time-string->pretty-date [time-string]
  (time-format/unparse pretty-date (time-string->time-stamp time-string)))

