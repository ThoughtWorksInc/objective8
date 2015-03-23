(ns midje-config
  (:require [clj-logging-config.jul :refer [set-loggers! set-logger!]]))

(prn "Turning off logging for tests")

(set-logger! :root
             {:level :off})
