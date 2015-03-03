(ns objective8.invitations
  (:require [objective8.storage.storage :as storage]
            [objective8.storage.mappings :as mappings]))

(defn get-active-invitation
  "Returns the invitation with the given uuid if it is active, otherwise returns nil"
  [uuid]
  (-> (storage/pg-retrieve {:entity :invitation
                            :uuid uuid
                            :status (mappings/string->postgres-type "invitation_status" "active")})
      :result
      first))

(defn accept-invitation! [invitation]
  (storage/pg-update-invitation-status! invitation "accepted"))
