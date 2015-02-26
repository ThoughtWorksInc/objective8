(ns objective8.writers
  (:require 
    [objective8.utils :as utils]    
    [objective8.storage.storage :as storage]))  

(defn store-invitation! [invitation]
  (storage/pg-store! (assoc invitation 
                            :entity :invitation
                            :status "active"
                            :uuid (utils/generate-random-uuid))))

(defn retrieve-invitation-by-uuid [uuid]
  (let [{result :result} (storage/pg-retrieve {:entity :invitation :uuid uuid})]
    (dissoc (first result) :entity)))

(defn retrieve-candidates [objective-id]
  (let [{result :result} (storage/pg-retrieve {:entity :candidate :objective-id objective-id}
                                              {:limit 50})]
    (map #(dissoc % :entity) result)))
