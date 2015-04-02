(ns objective8.actions
  (:require [objective8.objectives :as objectives]
            [objective8.invitations :as invitations]
            [objective8.writers :as writers]
            [objective8.drafts :as drafts]
            [objective8.up-down-votes :as up-down-votes]
            [objective8.comments :as comments]
            [objective8.storage.storage :as storage]))

(defn start-drafting! [objective-id]
  (let [objective (storage/pg-retrieve-entity-by-uri (str "/objectives/" objective-id) :with-global-id)]
    (doall (->> (invitations/retrieve-active-invitations objective-id)
                (map invitations/expire-invitation!)))
    (storage/pg-update-objective-status! objective "drafting")))

(defn update-objectives-due-for-drafting! []
  (doall (->> (objectives/retrieve-objectives-due-for-drafting)
              (map #(start-drafting! (:_id %))))))

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

(defn can-comment-on? [{:keys [entity] :as entity-to-post-to}]
    (case entity
      :objective (= "open" (:status entity-to-post-to))
      :draft true
      false))

(defn allowed-to-vote? [{:keys [global-id entity] :as entity-to-vote-on} {:keys [created-by-id] :as vote-data}]
  (let [objective (objectives/retrieve-objective (:objective-id entity-to-vote-on))
        {owner-entity-type :entity} (if (= entity :comment)
                                      (storage/pg-retrieve-entity-by-uri (:comment-on-uri entity-to-vote-on))
                                      entity-to-vote-on)]
    (and (case owner-entity-type
           :draft (objectives/in-drafting? objective)
           :objective (objectives/open? objective)
           :answer (objectives/open? objective)
           false)
         (not (up-down-votes/get-vote global-id created-by-id)))))

(defn cast-up-down-vote! [{:keys [vote-on-uri created-by-id vote-type] :as vote-data}]
  (if-let [{global-id :global-id :as entity-to-vote-on} (storage/pg-retrieve-entity-by-uri vote-on-uri :with-global-id)]
    (if (allowed-to-vote? entity-to-vote-on vote-data)
      (when-let [stored-vote (up-down-votes/store-vote! entity-to-vote-on vote-data)]
        {:status ::success :result stored-vote})
      {:status ::forbidden})
    {:status ::entity-not-found}))

(defn create-comment! [{:keys [comment-on-uri] :as comment-data}]
  (if-let [entity-to-comment-on (storage/pg-retrieve-entity-by-uri comment-on-uri :with-global-id)]
    (if (can-comment-on? entity-to-comment-on)
      (if-let [stored-comment (comments/store-comment-for! entity-to-comment-on comment-data)]
        {:status ::success :result stored-comment}
        {:status ::failure})
      {:status ::objective-drafting-started})
    {:status ::entity-not-found}))

(defn get-comments [entity-uri]
  (if-let [results (comments/get-comments entity-uri)]
    {:status ::success :result results}
    {:status ::entity-not-found}))
