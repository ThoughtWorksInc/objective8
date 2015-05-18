(ns objective8.front-end.api.domain
  (:require [objective8.config :as config]))

(defn open? [objective]
  (if config/two-phase?
    (= "open" (:status objective))
    true))

(defn in-drafting? [objective]
  (not (and (open? objective)
            config/two-phase?)))

(defn starred? [objective]
  (get-in objective [:meta :starred]))

(defn marked? [question]
  (get-in question [:meta :marked]))
