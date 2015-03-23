(ns objective8.integration.db.storage
  (:require [midje.sweet :refer :all]
            [objective8.storage.storage :as storage]
            [objective8.storage.database :as db]
            [objective8.integration.integration-helpers :refer [truncate-tables
                                                                db-connection]]
            [objective8.integration.storage-helpers :as sh]))

(facts "Storage tests"
       (against-background
        [(before :contents (do (db-connection)
                               (truncate-tables)))
         (after :facts (truncate-tables))]

        (facts "About storage"

               ;;GENERAL
               (fact "pg-store! throws org.postgresql.util.PSQLException when insert fails"
                     (let [NOT-EXISTENT-USER-ID 1
                           objective {:entity :objective
                                      :created-by-id NOT-EXISTENT-USER-ID
                                      :end-date "2015-01-01T00:00:00Z"
                                      :description "description"
                                      :goals "goals"
                                      :title "title"}]
                       (storage/pg-store! objective) => (throws org.postgresql.util.PSQLException)))

               ;;USERS
               (fact "a user entity can be stored in the database"
                     (let [user {:entity :user :twitter-id "the-twitter-id" :username "testname1" :some-key "some-value"}
                           store-result (storage/pg-store! user)
                           retrieve-result (storage/pg-retrieve {:entity :user :_id (:_id store-result)})]
                       (first (:result retrieve-result))) => (contains {:twitter-id "the-twitter-id"
                                                                        :username "testname1"
                                                                        :some-key "some-value"}))

               (fact "attempting to store a non-unique username throws an exception"
                     (let [user1 {:entity :user :twitter-id "the-twitter-id" :username "SAMEUSERNAME"}
                           _ (storage/pg-store! user1)
                           user2 {:entity :user :twitter-id "another-twitter-id" :username "SAMEUSERNAME"}]
                       (storage/pg-store! user2)) => (throws org.postgresql.util.PSQLException)) 

               (fact "a user entity can be retrieved by username"
                     (let [user {:entity :user :twitter-id "the-twitter-id" :username "testname1" :some-key "some-value"}
                           store-result (storage/pg-store! user)
                           retrieve-result (storage/pg-retrieve {:entity :user :username "testname1"})]
                       (first (:result retrieve-result))) => (contains {:twitter-id "the-twitter-id"
                                                                        :username "testname1"
                                                                        :some-key "some-value"}))

               ;;OBJECTIVES

               (defn store-an-objective-by [created-by-id]
                  (let [objective {:entity :objective
                                   :created-by-id created-by-id
                                   :end-date "2015-01-01T00:00:00Z"
                                   :description "description"
                                   :goals "goals"
                                   :title "title"}]
                    (storage/pg-store! objective)))

               (fact "an objective entity can be stored in the database"
                     (let [{user-id :_id username :username} (sh/store-a-user)
                           store-result (store-an-objective-by user-id)
                           retrieve-result (storage/pg-retrieve {:entity :objective :_id (:_id store-result)})]
                       (first (:result retrieve-result)) => (contains {:created-by-id user-id
                                                                       :username username
                                                                       :end-date "2015-01-01T00:00:00Z"
                                                                       :title "title"})))

               (fact "the 'drafting-status' of an objective can be updated"
                     (let [objective (sh/store-an-objective)]
                      (:drafting-started (storage/pg-update-objective-status! objective true)) => true)) 

               ;;COMMENTS
               (fact "a comment entity can be stored in the database"
                     (let [{user-id :_id username :username} (sh/store-a-user)
                           {objective-id :_id comment-on-id :global-id} (sh/store-an-objective)
                           comment {:entity :comment
                                    :created-by-id user-id
                                    :objective-id objective-id
                                    :comment-on-id comment-on-id
                                    :comment "A comment"}
                           store-result (storage/pg-store! comment)
                           retrieve-result (storage/pg-retrieve {:entity :comment :_id (:_id store-result)})]
                       (first (:result retrieve-result)) => (contains {:created-by-id user-id
                                                                       :username username
                                                                       :objective-id objective-id
                                                                       :comment-on-id comment-on-id})))

               ;;QUESTIONS
               (fact "a question entity can be stored in the database"
                     (let [{user-id :_id username :username} (sh/store-a-user)
                           {objective-id :_id} (sh/store-an-objective)
                           question {:entity :question
                                     :created-by-id user-id
                                     :objective-id objective-id
                                     :question "A question"}
                           store-result (storage/pg-store! question)
                           retrieve-result (storage/pg-retrieve {:entity :question :_id (:_id store-result)})]
                       (first (:result retrieve-result)) => (contains {:created-by-id user-id
                                                                       :username username
                                                                       :objective-id objective-id})))
               (fact "questions can be retrieved by objective ID"
                     (let [{user-id :_id username :username} (sh/store-a-user)
                           {an-objective-id :_id} (sh/store-an-objective)
                           {another-objective-id :_id} (sh/store-an-objective)
                           question-1 {:entity :question :created-by-id user-id :objective-id an-objective-id :question "A question"}
                           question-2 {:entity :question :created-by-id user-id :objective-id an-objective-id :question "A question"}
                           question-3 {:entity :question :created-by-id user-id :objective-id another-objective-id :question "Another question"}
                           stored-question-1 (storage/pg-store! question-1)
                           stored-question-2 (storage/pg-store! question-2)
                           stored-question-3 (storage/pg-store! question-3)
                           retrieve-result (storage/pg-retrieve {:entity :question :objective_id an-objective-id})]
                       (:result retrieve-result) => [(assoc stored-question-1 :username username)
                                                     (assoc stored-question-2 :username username)]))

               ;;ANSWERS
               (fact "an answer entity can be stored in the database"
                     (let [{user-id :_id username :username} (sh/store-a-user)
                           {question-id :_id objective-id :objective-id} (sh/store-a-question)
                           answer {:entity :answer
                                   :created-by-id user-id
                                   :question-id question-id
                                   :objective-id objective-id
                                   :answer "An answer"}
                           store-result (storage/pg-store! answer)
                           retrieve-result (storage/pg-retrieve {:entity :answer
                                                                 :_id (:_id store-result)})]
                       (first (:result retrieve-result)) => (contains {:created-by-id user-id
                                                                       :username username
                                                                       :question-id question-id})))

               ;;UP-DOWN-VOTES
               (fact "an up-down-vote can be stored in the database"
                     (let [{:keys [global-id created-by-id]} (sh/store-an-answer)
                           up-down-vote {:entity :up-down-vote
                                         :created-by-id created-by-id
                                         :global-id global-id
                                         :vote-type :up}
                           store-result (storage/pg-store! up-down-vote)
                           retrieve-result (storage/pg-retrieve {:entity :up-down-vote
                                                                 :_id (:_id store-result)})]
                       (first (:result retrieve-result)) => (contains {:created-by-id created-by-id
                                                                       :global-id global-id
                                                                       :vote-type :up})))
               
               ;;INVITATIONS
               (fact "an invitation entity can be stored in the database"
                     (let [{user-id :_id} (sh/store-a-user)
                           {objective-id :_id} (sh/store-an-objective)
                           invitation {:entity :invitation
                                       :invited-by-id user-id
                                       :objective-id objective-id
                                       :status "active"
                                       :writer-name "barry"
                                       :reason "he's barry"
                                       :uuid "random-uuid"}
                           store-result (storage/pg-store! invitation)
                           retrieve-result (storage/pg-retrieve {:entity :invitation :_id (:_id store-result)})]
                       (first (:result retrieve-result)) => (contains {:invited-by-id user-id
                                                                       :objective-id objective-id
                                                                       :uuid "random-uuid"
                                                                       :status "active"})))

               (fact "an invitation can be retrieved by uuid"
                     (let [user-id (:_id (sh/store-a-user))
                           objective-id (:_id (storage/pg-store! {:entity :objective
                                                                  :created-by-id user-id
                                                                  :end-date "2015-01-01"}))
                           invitation (storage/pg-store! {:entity :invitation
                                                          :invited-by-id user-id
                                                          :objective-id objective-id
                                                          :status "active"
                                                          :uuid "the-uuid"})]
                       (storage/pg-retrieve {:entity :invitation :uuid "the-uuid"}) => (contains {:result (contains invitation)})))

               ;;CANDIDATES
               (fact "a candidate entity can be stored in the database"
                     (let [{user-id :_id} (sh/store-a-user)
                           {objective-id :_id} (sh/store-an-objective)
                           {invitation-id :_id} (sh/store-an-invitation)
                           candidate {:entity :candidate
                                      :user-id user-id
                                      :objective-id objective-id
                                      :invitation-id invitation-id}
                           store-result (storage/pg-store! candidate)
                           retrieve-result (storage/pg-retrieve {:entity :candidate :_id (:_id store-result)})]
                       (first (:result retrieve-result)) => (contains {:user-id user-id
                                                                       :objective-id objective-id
                                                                       :invitation-id invitation-id})))

               ;; INVITATIONS
               (facts "about invitations"
                      (fact "an invitation's status can be updated"
                            (let [invitation (sh/store-an-invitation)]
                              (:status (storage/pg-update-invitation-status! invitation "accepted")) => "accepted")))

               ;;DRAFTS
               (facts "about drafts"
                      (fact "a draft can be stored in the database"
                            (let [{objective-id :objective-id submitter-id :user-id} (sh/store-a-candidate)
                                  draft {:entity :draft
                                         :content "Some content"
                                         :submitter-id submitter-id
                                         :objective-id objective-id}
                                  stored-draft (storage/pg-store! draft)
                                  retrieve-result (storage/pg-retrieve {:entity :draft :_id (:_id stored-draft)})]
                              (first (:result retrieve-result)) => (contains {:objective-id objective-id
                                                                              :submitter-id submitter-id
                                                                              :content "Some content"}))))

               ;;BEARER-TOKENS
               (facts "about bearer-tokens"
                      (fact "a bearer-token entity can be stored in the database"
                            (let [bearer-token {:entity :bearer-token
                                                :bearer-name "bearer name"
                                                :bearer-token "123"
                                                :authoriser true}]
                              (storage/pg-store! bearer-token)
                              (let [retrieve-result (storage/pg-retrieve {:entity :bearer-token :bearer-name "bearer name"})]
                                (first (:result retrieve-result)) => (contains {:bearer-name "bearer name"}))))

                      (fact "a bearer-token entity can be update in the database"
                            (let [bearer-token {:entity :bearer-token
                                                :bearer-name "bearer name"
                                                :bearer-token "123"}]
                              (storage/pg-store! bearer-token)
                              (storage/pg-update-bearer-token! (assoc bearer-token :bearer-token "new-token"))
                              (let [retrieve-result (storage/pg-retrieve {:entity :bearer-token :bearer-name "bearer name"})]
                                (first (:result retrieve-result)) => (contains {:bearer-token "new-token"})))))

               ;;RETRIEVING MANY RESULTS
               (facts "about retrieving"
                      (fact "all results are retrieved when no limit is supplied"
                            (let [users [{:entity :user :twitter-id "the-twitter-id1" :username "username1"}
                                         {:entity :user :twitter-id "the-twitter-id2" :username "username2"}
                                         {:entity :user :twitter-id "the-twitter-id3" :username "username3"}]
                                  store-result (doall (map storage/pg-store! users))
                                  retrieve-result (storage/pg-retrieve {:entity :user})]
                              (count (:result retrieve-result)) => 3))

                      (fact "can limit the number of results when retrieving"
                            (let [users [{:entity :user :twitter-id "the-twitter-id1" :username "username1"}
                                         {:entity :user :twitter-id "the-twitter-id2" :username "username2"}
                                         {:entity :user :twitter-id "the-twitter-id3" :username "username3"}]
                                  store-result (doall (map storage/pg-store! users))
                                  retrieve-result (storage/pg-retrieve {:entity :user} {:limit 2})]
                              (count (:result retrieve-result)) => 2))

                      (fact "results are retrieved in chronological order by default"
                            (let [users [{:entity :user :twitter-id "the-twitter-id1" :username "username1"}
                                         {:entity :user :twitter-id "the-twitter-id2" :username "username2"}
                                         {:entity :user :twitter-id "the-twitter-id3" :username "username3"}]
                                  store-result (doall (map storage/pg-store! users))
                                  retrieved-users (:result (storage/pg-retrieve {:entity :user}))
                                  sorted-twitter-ids (vec (map :twitter-id retrieved-users))]
                              sorted-twitter-ids)
                            => ["the-twitter-id1" "the-twitter-id2" "the-twitter-id3"])

                      (fact "results can be retrieved in reverse order"
                            (let [users [{:entity :user :twitter-id "the-twitter-id1" :username "username1"}
                                         {:entity :user :twitter-id "the-twitter-id2" :username "username2"}
                                         {:entity :user :twitter-id "the-twitter-id3" :username "username3"}]
                                  store-result (doall (map storage/pg-store! users))
                                  retrieved-users (:result (storage/pg-retrieve {:entity :user} {:sort {:field :_created_at
                                                                                                        :ordering :DESC}}))
                                  sorted-twitter-ids (vec (map :twitter-id retrieved-users))]
                              sorted-twitter-ids)
                            => ["the-twitter-id3" "the-twitter-id2" "the-twitter-id1"])))))

(facts "about entity specific queries"
       (against-background
        [(before :contents (do (db-connection)
                               (truncate-tables)))
         (after :facts (truncate-tables))]
        (fact "retrieving answers for a question also returns aggregate up-down-votes for those answers"
              (let [question (sh/store-a-question)
                    {g-id-1 :global-id :as answer-1} (sh/store-an-answer {:question question})
                    {g-id-2 :global-id :as answer-2} (sh/store-an-answer {:question question})
                    votes-for-answer-1 [:up :up :down :down :down]
                    votes-for-answer-2 [:up :up]
                    stored-votes-for-answer-1 (doall (map #(sh/store-an-up-down-vote g-id-1 %) votes-for-answer-1))
                    stored-votes-for-answer-2 (doall (map #(sh/store-an-up-down-vote g-id-2 %) votes-for-answer-2))]
                (storage/pg-retrieve-answers-with-votes-for-question (:_id question))
                => (contains [(contains (assoc answer-1 :votes {:up 2 :down 3}))
                              (contains (assoc answer-2 :votes {:up 2 :down 0}))])))
         
         (fact "retrieving a draft returns _created_at_sql_time for accurate time comparison between drafts"
               (let [draft (sh/store-a-draft)
                     retrieved-draft (first (:result (storage/pg-retrieve {:entity :draft :_id (:_id draft)})))]
                 (:_created_at_sql_time retrieved-draft)) =not=> nil)))
