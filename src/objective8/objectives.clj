(ns objective8.objectives
  (:require [objective8.storage.storage :as storage]))

(def open? (complement :drafting-started))

(defn store-objective! [objective]
  (storage/pg-store! (assoc objective :entity :objective)))

(defn retrieve-objective [objective-id]
  (-> (storage/pg-retrieve {:entity :objective :_id objective-id})
      :result
      first))

(defn retrieve-objectives []
  (:result (storage/pg-retrieve {:entity :objective}
                                {:limit 50
                                 :sort {:field :_created_at
                                        :ordering :DESC}})))
