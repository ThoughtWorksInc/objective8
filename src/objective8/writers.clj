(ns objective8.writers
  (:require 
    [objective8.utils :as utils]    
    [objective8.storage.storage :as storage]))  

(defn store-invited-writer! [writer]
  (storage/pg-store! (assoc writer 
                            :entity :invitation
                            :uuid (utils/generate-random-uuid))))

(defn retrieve-invitation-by-uuid [uuid]
  (let [{result :result} (storage/pg-retrieve {:entity :invitation :uuid uuid})]
    (dissoc (first result) :entity)))
