(ns objective8.actions
  (:require [objective8.objectives :as objectives]
            [objective8.invitations :as invitations]
            [objective8.writers :as writers]
            [objective8.drafts :as drafts]
            [objective8.up-down-votes :as up-down-votes]
            [objective8.storage.storage :as storage]))

(defn start-drafting! [objective-id]
  (let [objective (objectives/retrieve-objective objective-id)]

    (doall (->> (invitations/retrieve-active-invitations objective-id)
                (map invitations/expire-invitation!)))
    (storage/pg-update-objective-status! objective true)))

(defn submit-draft! [{:keys [submitter-id objective-id] :as draft-data}]
  (when (objectives/in-drafting? (objectives/retrieve-objective objective-id)) 
    (when (writers/retrieve-candidate-for-objective submitter-id objective-id)
      (drafts/store-draft! draft-data))))

(defn cast-up-down-vote! [{:keys [ueid user-id vote-type] :as vote-data}]
  (let [previous-vote (up-down-votes/get-active-vote ueid user-id)]
    (when-not (= (:vote-type previous-vote) vote-type)
      (if previous-vote
        (up-down-votes/update-vote! previous-vote vote-data)
        (up-down-votes/store-vote! vote-data)))))
