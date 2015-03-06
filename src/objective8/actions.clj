(ns objective8.actions
  (:require [objective8.objectives :as objectives]
            [objective8.invitations :as invitations]
            [objective8.storage.storage :as storage]))

(defn start-drafting! [objective-id]
  (let [objective (objectives/retrieve-objective objective-id)]

    (doall (->> (invitations/retrieve-active-invitations objective-id)
                (map invitations/expire-invitation!)))
    (storage/pg-update-objective-status! objective true)))
