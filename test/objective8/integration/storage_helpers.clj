(ns objective8.integration.storage-helpers
  (:require [clj-time.core :as tc]
            [objective8.objectives :as objectives]
            [objective8.users :as users]
            [objective8.storage.storage :as storage]
            [objective8.actions :as actions]))


(defmacro l-get [m k default]
  `(if (get ~m ~k)
     (get ~m ~k)
     ~default))

(def username-index (atom 0))

(defn generate-unique-username []
  (str "username-" (swap! username-index inc)))

(defn store-a-user [] (storage/pg-store! {:entity :user
                                          :twitter-id "twitter-TWITTER_ID"
                                          :username (generate-unique-username)
                                          :profile {:name "Barry"
                                                    :biog "I'm Barry..."}}))

(defn store-an-admin []
  (let [{twitter-id :twitter-id :as user} (store-a-user)]
    (users/store-admin! {:twitter-id twitter-id})
    user))

(defn store-an-open-objective 
  
  ([]
   (store-an-open-objective {}))

  ([entities]
   (let [{user-id :_id} (l-get entities :user (store-a-user))]
     (storage/pg-store! {:entity :objective
                         :status "open"
                         :removed-by-admin false
                         :title "test title"
                         :description "test description"
                         :created-by-id user-id
                         :end-date (str (tc/from-now (tc/days 1)))}))))

(defn store-an-objective-due-for-drafting
([] 
 (store-an-objective-due-for-drafting {})) 
 ([required-entities] 
  (let [{user-id :_id} (l-get required-entities :user (store-a-user))]
    (storage/pg-store! {:entity :objective
                        :created-by-id user-id
                        :status "open"
                        :removed-by-admin false
                        :end-date (str (tc/ago (tc/days 1)))}))))

(defn store-an-objective-in-draft []
  (let [{user-id :_id} (store-a-user)]
    (storage/pg-store! {:entity :objective
                        :created-by-id user-id
                        :end-date (str (tc/ago (tc/days 1))) 
                        :removed-by-admin false
                        :status "drafting"})))

(defn store-a-comment
  ([]
   (store-a-comment {:user (store-a-user) :entity (store-an-open-objective)}))

  ([required-entities]
   (let [{created-by-id :_id} (l-get required-entities :user (store-a-user))
         {:keys [_id objective-id global-id entity]} (l-get required-entities :entity (store-an-open-objective))
         objective-id (if (= entity :objective) _id objective-id)]
     (storage/pg-store! {:entity :comment
                         :created-by-id created-by-id
                         :objective-id objective-id
                         :comment-on-id global-id
                         :comment "The comment"}))))

(defn store-an-invitation
  ([] (store-an-invitation {}))

  ([required-entities]
   (let [{invited-by-id :_id} (l-get required-entities :user (store-a-user))
         {objective-id :_id} (l-get required-entities :objective (store-an-open-objective))
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
         {objective-id :_id} (l-get required-entities :objective (store-an-open-objective))]
     (storage/pg-store! {:entity :question
                         :created-by-id created-by-id
                         :objective-id objective-id
                         :question "A question"}))))

(defn store-an-answer
  ([]
   (store-an-answer {}))

  ([required-entities]
   (let [{created-by-id :_id} (l-get required-entities :user (store-a-user))
         {objective-id :objective-id q-id :_id} (l-get required-entities :question (store-a-question))]
     (storage/pg-store! {:entity :answer
                         :created-by-id created-by-id
                         :objective-id objective-id
                         :question-id q-id
                         :answer "An answer"}))))

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
         {i-id :_id o-id :objective-id} (l-get required-entities :invitation (store-an-invitation))]
     (storage/pg-store! {:entity :writer
                         :user-id user-id
                         :objective-id o-id
                         :invitation-id i-id}))))

(defn store-a-note
  ([]
   (store-a-note {}))

  ([required-entities]
   (let [{answer-id :global-id user-id :created-by-id o-id :objective-id} (l-get required-entities :answer (store-an-answer))]
     (storage/pg-store! {:entity :writer-note
                         :objective-id o-id
                         :created-by-id user-id
                         :note-on-id answer-id
                         :note "Test note"}))))

(def some-hiccup '(["h1" {:data-section-label "1234abcd"} "A Heading"] ["p" {:data-section-label "abcd1234"} "A paragraph"]))

(defn store-a-draft
  ([]
   (store-a-draft {}))

  ([required-entities]
   (let [{objective-id :_id} (l-get required-entities :objective (store-an-objective-in-draft))
         ;; NB: Writer id not required, but for consistency, the submitter should be authorised to draft documents for this objective
         {submitter-id :user-id} (l-get required-entities :submitter (store-a-writer))
         content (get required-entities :content some-hiccup)]
     (storage/pg-store! {:entity :draft
                         :submitter-id submitter-id
                         :objective-id objective-id
                         :content content}))))

(defn start-drafting! [objective-id]
  (actions/start-drafting! objective-id)) 

(defn retrieve-invitation [invitation-id]
  (-> (storage/pg-retrieve {:entity :invitation :_id invitation-id}) 
      :result
      first))

(defn store-a-star 
  ([]
   (store-a-star {})) 

  ([entities]
   (let [{objective-id :_id} (l-get entities :objective (store-an-open-objective))
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

