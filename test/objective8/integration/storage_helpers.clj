(ns objective8.integration.storage-helpers
  (:require [clj-time.core :as tc]
            [objective8.back-end.domain.objectives :as objectives]
            [objective8.back-end.domain.activities :as activities]
            [objective8.back-end.domain.users :as users]
            [objective8.back-end.storage.storage :as storage]
            [objective8.back-end.actions :as actions]))


(defmacro l-get [m k default]
  `(if (get ~m ~k)
     (get ~m ~k)
     ~default))

(def username-index (atom 0))

(defn generate-unique-username []
  (str "username-" (swap! username-index inc)))

(defn store-a-user [] (storage/pg-store! {:entity :user
                                          :auth-provider-user-id "twitter-TWITTER_ID"
                                          :username (generate-unique-username)
                                          :profile {:name "Barry"
                                                    :biog "I'm Barry..."}}))

(defn store-an-admin []
  (let [{auth-provider-user-id :auth-provider-user-id :as user} (store-a-user)]
    (users/store-admin! {:auth-provider-user-id auth-provider-user-id})
    user))

(defn store-an-objective

  ([]
   (store-an-objective {}))

  ([entities]
   (let [{user-id :_id} (l-get entities :user (store-a-user))
         title (get entities :title "test title")
         description (get entities :description "test description")]
     (storage/pg-store! {:entity :objective
                         :removed-by-admin false
                         :title title
                         :description description
                         :created-by-id user-id}))))

(defn store-a-promoted-objective

  ([]
   (store-a-promoted-objective {}))

  ([entities]
   (let [{user-id :_id} (l-get entities :user (store-a-user))
         title (get entities :title "test promoted title")
         description (get entities :description "test promoted description")]
     (storage/pg-store! {:entity :objective
                         :removed-by-admin false
                         :title title
                         :description description
                         :created-by-id user-id
                         :promoted true}))))

(defn store-an-admin-removed-objective

  ([]
   (store-an-admin-removed-objective {}))

  ([entities]
   (let [{user-id :_id} (l-get entities :user (store-a-user))]
     (storage/pg-store! {:entity :objective
                         :removed-by-admin true
                         :title "test admin removed title"
                         :description "test admin removed description"
                         :created-by-id user-id}))))

(defn store-an-activity []
 (-> (store-an-objective)
     :_id
     objectives/get-objective
     activities/store-activity!))

(defn store-an-invitation
  ([] (store-an-invitation {}))

  ([required-entities]
   (let [{invited-by-id :_id} (l-get required-entities :user (store-a-user))
         {objective-id :_id} (l-get required-entities :objective (store-an-objective))
         status (get required-entities :status "active")]
     (storage/pg-store! {:entity :invitation
                         :uuid (java.util.UUID/randomUUID)
                         :status status
                         :invited-by-id invited-by-id
                         :objective-id objective-id
                         :reason "some reason"
                         :writer-name "writer name"}))))

(defn store-a-question
  ([]
   (store-a-question {}))

  ([required-entities]
   (let [{created-by-id :_id} (l-get required-entities :user (store-a-user))
         {objective-id :_id} (l-get required-entities :objective (store-an-objective))
         question-text (get required-entities :question-text "A question")]
     (storage/pg-store! {:entity :question
                         :created-by-id created-by-id
                         :objective-id objective-id
                         :question question-text}))))

(defn store-an-answer
  ([]
   (store-an-answer {}))

  ([required-entities]
   (let [{created-by-id :_id} (l-get required-entities :user (store-a-user))
         {objective-id :objective-id q-id :_id} (l-get required-entities :question (store-a-question))
         answer-text (get required-entities :answer-text "An answer")]
     (storage/pg-store! {:entity :answer
                         :created-by-id created-by-id
                         :objective-id objective-id
                         :question-id q-id
                         :answer answer-text}))))

(defn store-an-up-down-vote
  ([global-id vote-type]
   (store-an-up-down-vote global-id vote-type {}))

  ([global-id vote-type required-entities]
   (let [user (l-get required-entities :user (store-a-user))]
     (storage/pg-store! {:entity :up-down-vote
                         :global-id global-id
                         :vote-type vote-type
                         :created-by-id (:_id user)}))))

(defn with-votes [entity vote-counts]
  (doseq [vote-type [:up :down]
          _ (range (get vote-counts vote-type 0))]
    (store-an-up-down-vote (:global-id entity) vote-type))
  entity)

(defn store-a-writer
  ([]
   (store-a-writer {}))

  ([required-entities]
   (let [{user-id :_id} (l-get required-entities :user (store-a-user))
         objective (l-get required-entities :objective (store-an-objective))
         {i-id :_id o-id :objective-id} (l-get required-entities :invitation (store-an-invitation {:objective objective}))]
     (storage/pg-store! {:entity :writer
                         :user-id user-id
                         :objective-id o-id
                         :invitation-id i-id}))))

(defn store-a-note
  ([]
   (store-a-note {}))

  ([required-entities]
   (let [{answer-id :global-id o-id :objective-id} (l-get required-entities :note-on-entity (store-an-answer))
         {user-id :user-id} (l-get required-entities :writer (store-a-writer))
         note (get required-entities :note "Test note")]
     (storage/pg-store! {:entity :writer-note
                         :objective-id o-id
                         :created-by-id user-id
                         :note-on-id answer-id
                         :note note}))))

(defn with-note
  ([entity]
   (with-note entity "Test note"))

  ([entity note-text]
   (store-a-note {:note-on-entity entity :note note-text})
   entity))

(def some-hiccup '(["h1" {:data-section-label "1234abcd"} "A Heading"] ["p" {:data-section-label "abcd1234"} "A paragraph"]))

(defn store-a-draft
  ([]
   (store-a-draft {}))

  ([required-entities]
   (let [{objective-id :_id} (l-get required-entities :objective (store-an-objective))
         ;; NB: Writer id not required, but for consistency, the submitter should be authorised to draft documents for this objective
         {submitter-id :user-id} (l-get required-entities :submitter (store-a-writer))
         content (get required-entities :content some-hiccup)]
     (storage/pg-store! {:entity :draft
                         :submitter-id submitter-id
                         :objective-id objective-id
                         :content content}))))

(defn store-a-section
  ([] (store-a-section {}))

  ([entities]
   (let [draft (l-get entities :draft (store-a-draft))
         section-label (get entities :section-label "abcdefg")]
     (storage/pg-store! {:entity :section
                         :draft-id (:_id draft)
                         :objective-id (:objective-id draft)
                         :section-label section-label}))))

(defn store-a-comment
  ([]
   (store-a-comment {:user (store-a-user) :entity (store-an-objective)}))

  ([required-entities]
   (let [{created-by-id :_id} (l-get required-entities :user (store-a-user))
         {:keys [_id objective-id global-id entity]} (l-get required-entities :entity (store-an-objective))
         comment-text (get required-entities :comment-text "The comment")
         objective-id (if (= entity :objective) _id objective-id)]
     (storage/pg-store! {:entity :comment
                         :created-by-id created-by-id
                         :objective-id objective-id
                         :comment-on-id global-id
                         :comment comment-text}))))

(defn store-an-annotation
  ([] (store-an-annotation {}))

  ([entities]
   (let [section (l-get entities :section (store-a-section {:draft (store-a-draft)}))
         annotation-text (get entities :annotation-text "The annotation")
         reason (get entities :reason "general")]
     (let [stored-comment (store-a-comment {:entity section :comment-text annotation-text})
           stored-reason (storage/pg-store! {:entity :reason
                                             :comment-id (:_id stored-comment)
                                             :reason reason})]
       {:comment stored-comment
        :reason stored-reason}))))

(defn with-annotations [section-entity annotation-reasons]
  (doseq [reason annotation-reasons]
    (store-an-annotation {:section section-entity :reason reason}))
  section-entity)

(defn retrieve-invitation [invitation-id]
  (-> (storage/pg-retrieve {:entity :invitation :_id invitation-id})
      :result
      first))

(defn store-a-star
  ([]
   (store-a-star {}))

  ([entities]
   (let [{objective-id :_id} (l-get entities :objective (store-an-objective))
         {created-by-id :_id} (l-get entities :user (store-a-user))
         active (get entities :active true)]
     (storage/pg-store! {:entity :star
                         :created-by-id created-by-id
                         :objective-id objective-id
                         :active active}))))

(defn store-a-mark
  ([]
   (store-a-mark {}))

  ([entities]
   "Can provide :question or :question and :writer"
   (let [{question-id :_id objective-id :objective-id :as question} (l-get entities :question (store-a-question))]
     (let [{created-by-id :user-id :as writer} (l-get entities :writer
                                                      (store-a-writer
                                                        {:invitation (store-an-invitation
                                                                       {:objective {:_id objective-id}})}))
           active (get entities :active true)]
       (storage/pg-store! {:entity :mark
                           :created-by-id created-by-id
                           :question-id question-id
                           :active active})))))

(defn store-an-admin-removal
  ([]
   (store-an-admin-removal {}))

  ([entities]
   (let [{user-id :_id} (l-get entities :user (store-a-user))]
     (storage/pg-store! {:entity :admin-removal
                         :removed-by-id user-id
                         :removal-uri "/removal/uri"}))))
