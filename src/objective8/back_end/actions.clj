(ns objective8.back-end.actions
  (:require [objective8.back-end.domain.objectives :as objectives]
            [objective8.back-end.domain.invitations :as invitations]
            [objective8.back-end.domain.writers :as writers]
            [objective8.back-end.domain.drafts :as drafts]
            [objective8.back-end.domain.up-down-votes :as up-down-votes]
            [objective8.back-end.domain.comments :as comments]
            [objective8.back-end.domain.stars :as stars]
            [objective8.back-end.domain.users :as users]
            [objective8.back-end.domain.answers :as answers]
            [objective8.back-end.domain.questions :as questions]
            [objective8.back-end.domain.marks :as marks]
            [objective8.back-end.domain.activities :as activities]
            [objective8.back-end.domain.writer-notes :as writer-notes]
            [objective8.back-end.domain.admin-removals :as admin-removals]
            [objective8.back-end.storage.uris :as uris]
            [objective8.back-end.storage.storage :as storage]))

(defn create-writer-for-objective! [{:keys [created-by-id] :as objective}]
  (let
    [objective-id (:_id objective)
     {:keys [username profile] :as user} (users/retrieve-user (str "/users/" created-by-id))
     invitation {:invited-by-id (:created-by-id objective)
                 :objective-id  objective-id
                 :reason        "Default writer as creator of this objective"
                 :writer-name   username}
     {uuid :uuid} (invitations/store-invitation! invitation)
     writer {:invitation-uuid uuid
             :invitee-id      created-by-id}]
    (when-not profile
      (users/update-user! (assoc user :profile {:name username :biog (str "This profile was automatically generated for the creator of objective: " (:title objective))})))
    (writers/create-writer writer)))

(defn create-objective! [{:keys [created-by-id] :as objective}]
  (if-let [stored-objective (objectives/store-objective! objective)]
    (do (activities/store-activity! (objectives/get-objective (:_id stored-objective)))
        (create-writer-for-objective! stored-objective)
        {:result stored-objective
         :status ::success})
    {:status ::failure}))

(defn submit-draft! [{:keys [submitter-id objective-id] :as draft-data}]
  (when (writers/retrieve-writer-for-objective submitter-id objective-id)
    (drafts/store-draft! draft-data)))

(defn can-comment-on? [{:keys [entity] :as entity-to-post-to}]
  (case entity
    :objective true
    :draft true
    :section true
    false))

(defn allowed-to-vote? [{:keys [global-id entity] :as entity-to-vote-on} {:keys [created-by-id] :as vote-data}]
  (let [objective (objectives/get-objective (:objective-id entity-to-vote-on))
        {owner-entity-type :entity} (if (= entity :comment)
                                      (storage/pg-retrieve-entity-by-global-id (:comment-on-id entity-to-vote-on))
                                      entity-to-vote-on)]
    (and (case owner-entity-type
           :draft true
           :section true
           :objective true
           :answer true
           false)
         (not (up-down-votes/get-vote global-id created-by-id)))))

(defn cast-up-down-vote! [{:keys [vote-on-uri created-by-id vote-type] :as vote-data}]
  (if-let [{global-id :global-id :as entity-to-vote-on} (storage/pg-retrieve-entity-by-uri vote-on-uri :with-global-id)]
    (if (allowed-to-vote? entity-to-vote-on vote-data)
      (when-let [stored-vote (up-down-votes/store-vote! entity-to-vote-on vote-data)]
        {:status ::success :result stored-vote})
      {:status ::forbidden})
    {:status ::entity-not-found}))

(defn merge-comments-with-section [section]
  (let [comments (comments/get-comments (:uri section) {})]
    (assoc section :comments comments)))

(defn get-annotations-for-draft [draft-uri]
  (let [annotated-sections (drafts/get-annotated-sections-with-section-content draft-uri)]
    {:status ::success :result (map merge-comments-with-section annotated-sections)}))

(defn create-reason-for-comment! [reason stored-comment]
  (if-let [comment-with-reason (some->> {:comment-id (:_id stored-comment) :reason reason}
                                        comments/store-reason!
                                        :reason
                                        (assoc stored-comment :reason))]
    {:status ::success :result comment-with-reason}
    {:status ::failure}))

(defn create-section-comment! [{:keys [objective-id draft-id section-label] :as section-data} comment-data]
  (if-let [reason (:reason comment-data)]
    (let [section-labels (drafts/get-section-labels-for-draft-uri (str "/objectives/" objective-id "/drafts/" draft-id))]
      (if (some #{section-label} section-labels)
        (let [stored-section (drafts/store-section! section-data)
              stored-comment (comments/store-comment-for! stored-section comment-data)]
          (create-reason-for-comment! reason stored-comment))
        {:status ::entity-not-found}))
    {:status ::failure}))

(defn create-comment! [{:keys [comment-on-uri] :as comment-data}]
  (if-let [entity-to-comment-on (storage/pg-retrieve-entity-by-uri comment-on-uri :with-global-id)]
    (if (can-comment-on? entity-to-comment-on)
      (if-let [{comment-id :_id :as stored-comment} (comments/store-comment-for! entity-to-comment-on comment-data)]
        (if-let [reason (:reason comment-data)]
          (create-reason-for-comment! reason stored-comment)
          {:status ::success :result stored-comment})
        {:status ::failure})
      {:status ::failure})
    (if-let [section-data (uris/uri->section-data comment-on-uri)]
      (create-section-comment! section-data comment-data)
      {:status ::entity-not-found})))

(defn create-question! [{:keys [created-by-id objective-id] :as question}]
  (if (objectives/get-objective objective-id)
    (if-let [stored-question (questions/store-question! question)]
      (do (activities/store-activity! (questions/get-question (:uri stored-question)))
          (if (writers/retrieve-writer-for-objective created-by-id objective-id)
            {:status ::success :result (->> (marks/store-mark! {:question-uri   (:uri stored-question)
                                                                :created-by-uri (str "/users/" created-by-id)
                                                                :active         true})
                                            :active
                                            (assoc-in stored-question [:meta :marked]))}
            {:status ::success :result stored-question}))
      {:status ::failure})
    {:status ::entity-not-found}))

(defn create-answer! [{:keys [objective-id question-id] :as answer}]
  (let [question-uri (str "/objectives/" objective-id "/questions/" question-id)]
    (if (questions/get-question question-uri)
      (if-let [stored-answer (answers/store-answer! answer)]
        {:status ::success :result stored-answer}
        {:status ::failure})
      {:status ::entity-not-found})))

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
          objectives (objectives/get-objectives-owned-by-user-id (:_id user))
          admin (users/get-admin-by-auth-provider-user-id (:auth-provider-user-id user))]
      {:status ::success :result (assoc user
                                   :writer-records writers
                                   :owned-objectives objectives
                                   :admin (not (empty? admin)))})
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
  (if-let [objective (objectives/get-objective objective-id)]
    (if (some #{objective-id} (authorised-objectives-for-inviter invited-by-id))
      {:status ::success :result (invitations/store-invitation! invitation-data)}
      {:status ::failure})
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

(defn create-admin-removal! [{:keys [removal-uri removed-by-uri] :as admin-removal-data}]
  (if-let [user (users/retrieve-user removed-by-uri)]
    (if (users/get-admin-by-auth-provider-user-id (:auth-provider-user-id user))
      (if-let [objective (storage/pg-retrieve-entity-by-uri removal-uri :with-global-id)]
        (if (:removed-by-admin objective)
          {:status ::entity-not-found}
          (do
            (objectives/admin-remove-objective! objective)
            {:status ::success :result (admin-removals/store-admin-removal! admin-removal-data)}))
        {:status ::entity-not-found})
      {:status ::forbidden})
    {:status ::entity-not-found}))

(defn create-promote-objective! [{:keys [objective-uri promoted-by] :as promoted-objective-data}]
  (if-let [user (users/retrieve-user promoted-by)]
    (if (users/get-admin-by-auth-provider-user-id (:auth-provider-user-id user))
      (if-let [objective (storage/pg-retrieve-entity-by-uri objective-uri :with-global-id)]
        (if-let [toggled-objective (objectives/toggle-promoted-status! objective)]
          {:status ::success :result toggled-objective}
          {:status ::forbidden})
        {:status ::entity-not-found})
      {:status ::forbidden})
    {:status ::entity-not-found}))
