(ns objective8.config
 (:require [environ.core :as environ]))

(def ^:dynamic enable-csrf true)

(def get-var environ/env)
