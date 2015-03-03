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

(defn retrieve-invitation [invitation-id]
  (let [{result :result} (storage/pg-retrieve {:entity :invitation :_id invitation-id})]
    (dissoc (first result) :entity)))

(defn create-candidate [{:keys [invitation-uuid user-id] :as candidate-data}]
  (let [{:keys [name reason objective-id invited-by-id]
         invitation-id :_id} (storage/pg-update-invitation-status! invitation-uuid "accepted")]
    (storage/pg-store! {:entity :candidate
                        :objective-id objective-id
                        :invitation-id invitation-id
                        :invited-by-id invited-by-id
                        :writer-name name
                        :invitation-reason reason
                        :user-id user-id})))

(defn accept-invitation [{user-id :invitee-id :as invitation-response}]
  (let [updated-invitation (storage/pg-update-invitation-status! (:invitation-uuid invitation-response) "accepted")]
    (storage/pg-store! {:entity :candidate
                        :writer-name (:name updated-invitation)
                        :invitation-reason (:reason updated-invitation)
                        :objective-id (:objective-id updated-invitation)
                        :user-id user-id
                        :invitation-id (:_id updated-invitation)
                        :invited-by-id (:invited-by-id updated-invitation)})))

(defn retrieve-candidates [objective-id]
  (let [{result :result} (storage/pg-retrieve {:entity :candidate :objective-id objective-id}
                                              {:limit 50})]
    (map #(dissoc % :entity) result)))
