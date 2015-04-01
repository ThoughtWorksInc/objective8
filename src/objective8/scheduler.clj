(ns objective8.scheduler
  (:require [objective8.actions :as actions]))

(defn update-objectives []
  (actions/update-objectives-due-for-drafting!))
