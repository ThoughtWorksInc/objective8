(ns objective8.back-end.writers
  (:require [clojure.tools.logging :as log]
            [objective8.utils :as utils]
            [objective8.back-end.invitations :as i]
            [objective8.storage.storage :as storage]))  


(defn create-writer [{:keys [invitation-uuid invitee-id] :as writer-data}]
  (try
    (some-> (i/get-active-invitation invitation-uuid)
            i/accept-invitation!
            (utils/select-all-or-nothing [:writer-name :reason :objective-id :invited-by-id :_id]) 
            (utils/ressoc :_id :invitation-id)
            (utils/ressoc :reason :invitation-reason)
            (assoc :entity :writer :user-id invitee-id)
            storage/pg-store!)
   
    (catch org.postgresql.util.PSQLException e
      (throw (Exception. "Failed to create writer")))))

(defn retrieve-writers [objective-id]
  (:result (storage/pg-retrieve {:entity :writer 
                                 :objective-id objective-id}
                                {:limit 50})))

(defn retrieve-writers-by-user-id [user-id]
  (:result (storage/pg-retrieve {:entity :writer
                                 :user-id user-id}
                                {:limit 50})))

(defn retrieve-writer-for-objective [user-id objective-id]
  (first (:result (storage/pg-retrieve {:entity :writer
                                        :user-id user-id
                                        :objective-id objective-id}))))
