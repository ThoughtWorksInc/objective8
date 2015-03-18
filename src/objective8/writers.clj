(ns objective8.writers
  (:require [clojure.tools.logging :as log]
            [objective8.utils :as utils]
            [objective8.invitations :as i]
            [objective8.storage.storage :as storage]))  

(defn create-candidate [{:keys [invitation-uuid invitee-id] :as candidate-data}]
  (try
    (when-let [{:keys [writer-name
                       reason
                       objective-id
                       invited-by-id]
                invitation-id :_id} (some-> (i/get-active-invitation invitation-uuid)
                                            (i/accept-invitation!))]
      (storage/pg-store! {:entity :candidate
                          :objective-id objective-id
                          :invitation-id invitation-id
                          :invited-by-id invited-by-id
                          :writer-name writer-name
                          :invitation-reason reason
                          :user-id invitee-id}))
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
