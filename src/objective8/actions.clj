(ns objective8.actions
  (:require [objective8.objectives :as objectives]
            [objective8.invitations :as invitations]
            [objective8.writers :as writers]
            [objective8.drafts :as drafts]
            [objective8.up-down-votes :as up-down-votes]
            [objective8.comments :as comments]
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

(defn retrieve-drafts [objective-id]
  (if-let [objective (objectives/retrieve-objective objective-id)]
    (if (objectives/in-drafting? objective)
      {:status ::success :result (drafts/retrieve-drafts objective-id)} 
      {:status ::objective-drafting-not-started})
    {:status ::not-found}))

(defn retrieve-latest-draft [objective-id]
  (if (objectives/in-drafting? (objectives/retrieve-objective objective-id))
    {:status ::success :result (drafts/retrieve-latest-draft objective-id)}
    {:status ::objective-drafting-not-started}))

(defn cast-up-down-vote! [{:keys [global-id created-by-id vote-type] :as vote-data}]
  (when-not (up-down-votes/get-vote global-id created-by-id)
    (up-down-votes/store-vote! vote-data)))

(defn create-comment! [{:keys [comment-on-uri] :as comment-data}]
  (if-let [stored-comment (comments/store-comment! comment-data)]
    {:status ::success :result stored-comment}
    {:status ::failure}))

