(ns objective8.writers
  (:require [clojure.tools.logging :as log]
            [objective8.utils :as utils]
            [objective8.invitations :as i]
            [objective8.storage.storage :as storage]))  

(defn store-invitation! [invitation]
  (storage/pg-store! (assoc invitation 
                            :entity :invitation
                            :status "active"
                            :uuid (utils/generate-random-uuid))))

(defn retrieve-invitation-by-uuid [uuid]
  (let [{result :result} (storage/pg-retrieve {:entity :invitation :uuid uuid})]
    (dissoc (first result) :entity)))

(defn retrieve-invitation [invitation-id]
  (let [{result :result} (storage/pg-retrieve {:entity :invitation :_id invitation-id})]
    (dissoc (first result) :entity)))

(defn create-candidate [{:keys [invitation-uuid user-id] :as candidate-data}]
  (try
    (when-let [{:keys [name reason objective-id invited-by-id] invitation-id :_id} (some-> (i/get-active-invitation invitation-uuid)
                                                                                           (i/accept-invitation!))]
      (storage/pg-store! {:entity :candidate
                          :objective-id objective-id
                          :invitation-id invitation-id
                          :invited-by-id invited-by-id
                          :writer-name name
                          :invitation-reason reason
                          :user-id user-id}))
    (catch org.postgresql.util.PSQLException e
      (throw (Exception. "Failed to create candidate writer")))))

(defn retrieve-candidates [objective-id]
  (let [{result :result} (storage/pg-retrieve {:entity :candidate :objective-id objective-id}
                                              {:limit 50})]
    (map #(dissoc % :entity) result)))
