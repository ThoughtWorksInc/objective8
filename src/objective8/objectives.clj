(ns objective8.objectives
  (:require [objective8.storage.storage :as storage]))

(defn store-objective! [objective]
  (storage/pg-store! (assoc objective :entity :objective)))

(defn retrieve-objective [objective-id]
  (let [{result :result} (storage/pg-retrieve {:entity :objective :_id objective-id})]
    (dissoc (first result) :entity)))
