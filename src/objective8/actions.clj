(ns objective8.actions
  (:require [objective8.objectives :as objectives]
            [objective8.invitations :as invitations]
            [objective8.writers :as writers]
            [objective8.drafts :as drafts]
            [objective8.up-down-votes :as up-down-votes]
            [objective8.comments :as comments]
            [objective8.stars :as stars]
            [objective8.users :as users]
            [objective8.questions :as questions]
            [objective8.marks :as marks]
            [objective8.writer-notes :as writer-notes]
            [objective8.storage.uris :as uris]
            [objective8.storage.storage :as storage]))

(defn create-writer-for-objective! [{:keys [created-by-id] :as objective}]
  (let
    [objective-id (:_id objective)
     {:keys [username profile] :as user} (users/retrieve-user (str "/users/" created-by-id))
     invitation {:invited-by-id (:created-by-id objective)
                 :objective-id objective-id
                 :reason "Default writer as creator of this objective"
                 :writer-name username}
     {uuid :uuid} (invitations/store-invitation! invitation)
     writer {:invitation-uuid uuid
             :invitee-id created-by-id}]
    (when-not profile
      (users/update-user! (assoc user :profile {:name username :biog (str "This profile was automatically generated for the creator of objective: " (:title objective))})))
    (writers/create-writer writer)))

(defn create-objective! [{:keys [created-by-id] :as objective}]
  (if-let [stored-objective (objectives/store-objective! objective)]
    (do (create-writer-for-objective! stored-objective) 
        {:result stored-objective
         :status ::success})
    {:status ::failure}))

(defn get-objective-with-star-count [objective-id]
  (if-let [objective (objectives/retrieve-objective objective-id)] 
    (let [objective-uri (:uri objective) 
          star-count (stars/get-star-count-for-objective objective-uri)] 
      (update-in objective [:meta] merge star-count))))

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
    (when (writers/retrieve-writer-for-objective submitter-id objective-id)
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
    :section true
    false))

(defn allowed-to-vote? [{:keys [global-id entity] :as entity-to-vote-on} {:keys [created-by-id] :as vote-data}]
  (let [objective (objectives/retrieve-objective (:objective-id entity-to-vote-on))
        {owner-entity-type :entity} (if (= entity :comment)
                                      (storage/pg-retrieve-entity-by-global-id (:comment-on-id entity-to-vote-on))
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

(defn create-section-comment! [{:keys [draft-id section-label] :as section-data} comment-data]
  (let [section-labels (drafts/get-section-labels-for-draft draft-id)]
    (if (some #{section-label} section-labels)
      (let [stored-section (drafts/store-section! section-data)
            stored-comment (comments/store-comment-for! stored-section comment-data)]
        {:status ::success :result stored-comment}))))

(defn create-comment! [{:keys [comment-on-uri] :as comment-data}]
  (if-let [entity-to-comment-on (storage/pg-retrieve-entity-by-uri comment-on-uri :with-global-id)]
    (if (can-comment-on? entity-to-comment-on)
      (if-let [stored-comment (comments/store-comment-for! entity-to-comment-on comment-data)]
        {:status ::success :result stored-comment}
        {:status ::failure})
      {:status ::objective-drafting-started})
    (let [query (uris/uri->query comment-on-uri)]
      (if (= (:entity query) :section)
        (create-section-comment! query comment-data)
        {:status ::entity-not-found}))))

(defn get-comments [entity-uri]
  (if-let [results (comments/get-comments entity-uri)]
    {:status ::success :result results}
    {:status ::entity-not-found}))

(defn toggle-star! [{:keys [objective-uri created-by-id] :as star-data}]
  (if-let [{objective-id :_id} (storage/pg-retrieve-entity-by-uri objective-uri)]
    (if-let [star (stars/get-star objective-uri (str "/users/" created-by-id))]
      (if-let [toggled-star (stars/toggle-star! star)]
        {:status ::success :result toggled-star}
        {:status ::failure})
      (if-let [stored-star (stars/store-star! (assoc star-data :objective-id objective-id))]
        {:status ::success :result stored-star}
        {:status ::failure}))
    {:status ::entity-not-found}))

(defn mark-question! [mark-data]
  (if-let [question (questions/get-question (:question-uri mark-data))]
    (if-let [{active :active} (marks/get-mark-for-question (:question-uri mark-data))]
      {:status ::success
       :result (marks/store-mark! (assoc mark-data :active (not active)))}
      {:status ::success
       :result (marks/store-mark! (assoc mark-data :active true))})
    {:status ::entity-not-found}))

(defn get-user-with-roles [user-uri]
  (if-let [user (users/retrieve-user user-uri)]
    (let [writers (writers/retrieve-writers-by-user-id (:_id user))
          objectives (objectives/get-objectives-owned-by-user-id (:_id user))]
      {:status ::success :result (assoc user :writer-records writers :owned-objectives objectives)})
    {:status ::entity-not-found}))

(defn update-user-with-profile! [profile-data]
  (if-let [user (users/retrieve-user (:user-uri profile-data))]
    {:status ::success 
     :result (users/update-user! (assoc user :profile (dissoc profile-data :user-uri)))} 
    {:status ::entity-not-found}))

(defn authorised-objectives-for-inviter [user-id]
  (let [writer-objective-ids (map :objective-id (writers/retrieve-writers-by-user-id user-id))
        owned-objective-ids (map :_id (objectives/get-objectives-owned-by-user-id user-id))]
    (concat writer-objective-ids owned-objective-ids)))

(defn create-invitation! [{:keys [invited-by-id objective-id] :as invitation-data}]
  (if-let [objective (objectives/retrieve-objective objective-id)]
    (if (objectives/open? objective) 
      (if (some #{objective-id} (authorised-objectives-for-inviter invited-by-id)) 
        {:status ::success :result (invitations/store-invitation! invitation-data)}
        {:status ::failure})
      {:status ::objective-drafting-started}) 
    {:status ::entity-not-found}))

(defn authorised-objectives-for-note-writer [user-id]
  (map :objective-id (writers/retrieve-writers-by-user-id user-id)))

(defn writer-for-objective? [objective-id user-id]
  (some #{objective-id} (authorised-objectives-for-note-writer user-id)))

(defn create-writer-note! [{:keys [note-on-uri created-by-id] :as writer-note-data}]
  (if-let [{o-id :objective-id :as entity-to-note-on} (storage/pg-retrieve-entity-by-uri note-on-uri :with-global-id)]
    (if (empty? (writer-notes/retrieve-note note-on-uri)) 
      (if (writer-for-objective? o-id created-by-id)
        {:status ::success :result (writer-notes/store-note-for! entity-to-note-on writer-note-data)}
        {:status ::forbidden})
      {:status ::forbidden})
    {:status ::entity-not-found}))
