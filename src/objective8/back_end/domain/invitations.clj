(ns objective8.back-end.domain.invitations
  (:require [objective8.back-end.storage.storage :as storage]
            [objective8.back-end.storage.mappings :as mappings]
            [objective8.back-end.domain.objectives :as objectives]
            [objective8.utils :as utils]))

(defn store-invitation! [invitation]
  (storage/pg-store! (assoc invitation
                            :entity :invitation
                            :status "active"
                            :uuid (utils/generate-random-uuid))))

(defn get-active-invitation
  "Returns the invitation with the given uuid if it is active, otherwise returns nil"
  [uuid]
  (-> (storage/pg-retrieve {:entity :invitation
                            :uuid uuid
                            :status (mappings/string->postgres-type "invitation_status" "active")})
      :result
      first))

(defn get-invitation [uuid]
  (-> (storage/pg-retrieve {:entity :invitation
                            :uuid uuid})
      :result
      first))

(defn accept-invitation! [{objective-id :objective-id :as invitation}]
  (when (objectives/open? (objectives/get-objective objective-id))
    (storage/pg-update-invitation-status! invitation "accepted")))

(defn decline-invitation! [{objective-id :objective-id :as invitation}]
  (when (objectives/open? (objectives/get-objective objective-id))
    (storage/pg-update-invitation-status! invitation "declined")))

(defn decline-invitation-by-uuid [uuid]
  (some-> (get-active-invitation uuid)
          decline-invitation!))

(defn retrieve-active-invitations [objective-id]
  (-> (storage/pg-retrieve {:entity :invitation
                            :objective-id objective-id
                            :status (mappings/string->postgres-type "invitation_status" "active")})
      :result))

(defn expire-invitation! [invitation]
  (storage/pg-update-invitation-status! invitation "expired"))
