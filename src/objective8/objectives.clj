(ns objective8.objectives
  (:require [objective8.storage.storage :as storage]))

(def open? (complement :drafting-started))

(defn store-objective! [objective]
  (storage/pg-store! (assoc objective :entity :objective)))

(defn retrieve-objective [objective-id]
  (let [{result :result} (storage/pg-retrieve {:entity :objective :_id objective-id})]
    (dissoc (first result) :entity)))

(defn retrieve-objectives []
  (map #(dissoc % :entity)
       (:result (storage/pg-retrieve {:entity :objective}
                                     {:limit 50
                                      :sort {:field :_created_at
                                             :ordering :DESC}}))))

(defn start-drafting! [objective-id]
  (-> (retrieve-objective objective-id)
      (storage/pg-update-objective-status! true)))
