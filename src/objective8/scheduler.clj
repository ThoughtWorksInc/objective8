(ns objective8.scheduler
  (:require [chime :as chime]
            [clj-time.core :as time-core]
            [clj-time.periodic :as time-periodic]
            [objective8.back-end.actions :as actions]))

(defn update-objectives [time]
  (prn (str "Scheduler is updating objectives due for drafting at time:" time)) 
  (actions/update-objectives-due-for-drafting!)) 

(defn time-sequence [start-time time-interval]
  (time-periodic/periodic-seq start-time time-interval))

(def at-chime update-objectives)

(defn start-chime 
  ([checking-minutes]
   (start-chime checking-minutes (time-core/now))) 
  
  ([checking-minutes start-time] 
   (let [schedule (time-sequence start-time (time-core/minutes checking-minutes))] 
    (chime/chime-at schedule at-chime))))
