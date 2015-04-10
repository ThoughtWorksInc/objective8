(ns objective8.writers
  (:require [clojure.tools.logging :as log]
            [objective8.utils :as utils]
            [objective8.invitations :as i]
            [objective8.storage.storage :as storage]))  


(defn create-candidate [{:keys [invitation-uuid invitee-id] :as candidate-data}]
  (try
    (some-> (i/get-active-invitation invitation-uuid)
            i/accept-invitation!
            (utils/select-all-or-nothing [:writer-name :reason :objective-id :invited-by-id :_id]) 
            (utils/ressoc :_id :invitation-id)
            (utils/ressoc :reason :invitation-reason)
            (assoc :entity :candidate :user-id invitee-id)
            storage/pg-store!)
   
    (catch org.postgresql.util.PSQLException e
      (throw (Exception. "Failed to create candidate writer")))))

(defn retrieve-candidates [objective-id]
  (:result (storage/pg-retrieve {:entity :candidate 
                                 :objective-id objective-id}
                                {:limit 50})))

(defn retrieve-candidates-by-user-id [user-id]
  (:result (storage/pg-retrieve {:entity :candidate
                                 :user-id user-id}
                                {:limit 50})))

(defn retrieve-candidate-for-objective [user-id objective-id]
  (first (:result (storage/pg-retrieve {:entity :candidate
                                        :user-id user-id
                                        :objective-id objective-id}))))
