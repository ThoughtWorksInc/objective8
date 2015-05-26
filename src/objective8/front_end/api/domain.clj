(ns objective8.front-end.api.domain
  (:require [objective8.config :as config]))

(defn starred? [objective]
  (get-in objective [:meta :starred]))

(defn marked? [question]
  (get-in question [:meta :marked]))
