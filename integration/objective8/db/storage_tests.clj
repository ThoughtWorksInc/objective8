(ns objective8.db.storage-tests
  (:require [midje.sweet :refer :all]
            [objective8.storage.storage :as storage]
            [objective8.storage.database :as db]
            [objective8.integration-helpers :refer [truncate-tables
                                                    db-connection]]))
(defn store-a-user [] (storage/pg-store! {:entity :user
                                          :twitter-id "twitter-TWITTER_ID"
                                          :username "username"})) 
(facts "Storage tests" :integration
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
                (fact "an objective entity can be stored in the database"
                      (let [stored-user (store-a-user)
                            user-id (:_id stored-user)
                            objective {:entity :objective
                                       :created-by-id user-id
                                       :end-date "2015-01-01T00:00:00Z"
                                       :description "description"
                                       :goals "goals"
                                       :title "title"}
                            store-result (storage/pg-store! objective)
                            retrieve-result (storage/pg-retrieve {:entity :objective :_id (:_id store-result)})]
                        (first (:result retrieve-result)) => (contains {:created-by-id user-id
                                                                        :username "username"
                                                                        :end-date "2015-01-01T00:00:00Z"
                                                                        :title "title"})))
                ;;COMMENTS
                (fact "a comment entity can be stored in the database"
                      (let [stored-user (store-a-user)
                            user-id (:_id stored-user)
                            stored-objective (storage/pg-store! {:entity :objective
                                                                 :created-by-id user-id
                                                                 :end-date "2015-01-01T00:00:00.000Z"})
                            objective-id (:_id stored-objective)
                            comment {:entity :comment
                                     :created-by-id user-id
                                     :objective-id objective-id
                                     :comment "A comment"}
                            store-result (storage/pg-store! comment)
                            retrieve-result (storage/pg-retrieve {:entity :comment :_id (:_id store-result)})]
                        (first (:result retrieve-result)) => (contains {:created-by-id user-id
                                                                        :username "username"
                                                                        :objective-id objective-id})))

                ;;QUESTIONS
                (fact "a question entity can be stored in the database"
                      (let [stored-user (store-a-user)
                            user-id (:_id stored-user)
                            stored-objective (storage/pg-store! {:entity :objective
                                                                 :created-by-id user-id
                                                                 :end-date "2015-01-01T00:00:00.000Z"})
                            objective-id (:_id stored-objective)
                            question {:entity :question
                                      :created-by-id user-id
                                      :objective-id objective-id
                                      :question "A question"}
                            store-result (storage/pg-store! question)
                            retrieve-result (storage/pg-retrieve {:entity :question :_id (:_id store-result)})]
                        (first (:result retrieve-result)) => (contains {:created-by-id user-id
                                                                        :username "username"
                                                                        :objective-id objective-id}))) 
                (fact "questions can be retrieved by objective ID"
                      (let [stored-user (store-a-user)
                            user-id (:_id stored-user)
                            a-stored-objective (storage/pg-store! {:entity :objective
                                                                   :created-by-id user-id
                                                                   :end-date "2015-01-01T00:00:00.000Z"}) 
                            an-objective-id (:_id a-stored-objective)
                            another-stored-objective (storage/pg-store! {:entity :objective
                                                                         :created-by-id user-id
                                                                         :end-date "2015-02-01T00:00:00.000Z"})
                            another-objective-id (:_id another-stored-objective)
                            question-1 {:entity :question :created-by-id user-id :objective-id an-objective-id :question "A question"}
                            question-2 {:entity :question :created-by-id user-id :objective-id an-objective-id :question "A question"}
                            question-3 {:entity :question :created-by-id user-id :objective-id another-objective-id :question "Another question"}
                            stored-question-1 (storage/pg-store! question-1)
                            stored-question-2 (storage/pg-store! question-2)
                            stored-question-3 (storage/pg-store! question-3)
                            retrieve-result (storage/pg-retrieve {:entity :question :objective_id an-objective-id})]
                        (:result retrieve-result) => [(assoc stored-question-1 :username "username")
                                                      (assoc stored-question-2 :username "username")]))

;;ANSWERS
(fact "an answer entity can be stored in the database"
      (let [stored-user (store-a-user)
            user-id (:_id stored-user)
            stored-objective (storage/pg-store! {:entity :objective
                                                 :created-by-id user-id
                                                 :end-date "2015-01-01T00:00:00.000Z"})
            objective-id (:_id stored-objective)
            stored-question (storage/pg-store! {:entity :question
                                                :created-by-id user-id
                                                :objective-id objective-id
                                                :question "A question"})
            question-id (:_id stored-question)
            answer {:entity :answer
                    :created-by-id user-id
                    :question-id question-id
                    :answer "An answer"}
            store-result (storage/pg-store! answer)
            retrieve-result (storage/pg-retrieve {:entity :answer
                                                  :_id (:_id store-result)})]
        (first (:result retrieve-result)) => (contains {:created-by-id user-id
                                                        :username "username"
                                                        :question-id question-id})))

;;INVITATIONS
(fact "an invitation entity can be stored in the database"
      (let [stored-user (store-a-user)
            user-id (:_id stored-user)
            stored-objective (storage/pg-store! {:entity :objective
                                                 :created-by-id user-id
                                                 :end-date "2015-01-01T00:00:00.000Z"})
            objective-id (:_id stored-objective)
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
      (let [user-id (:_id (store-a-user))
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
      (let [stored-user (store-a-user)
            user-id (:_id stored-user)
            stored-objective (storage/pg-store! {:entity :objective
                                                 :created-by-id user-id
                                                 :end-date "2015-01-01T00:00:00.000Z"})
            objective-id (:_id stored-objective)
            stored-invitation (storage/pg-store! {:entity :invitation
                                                  :invited-by-id user-id
                                                  :objective-id objective-id
                                                  :status "active"
                                                  :writer-name "barry"
                                                  :reason "he's barry"
                                                  :uuid "random-uuid"})
            invitation-id (:_id stored-invitation)
            candidate {:entity :candidate
                       :user-id user-id
                       :objective-id objective-id
                       :invitation-id invitation-id}
            store-result (storage/pg-store! candidate)
            retrieve-result (storage/pg-retrieve {:entity :candidate :_id (:_id store-result)})]
        (first (:result retrieve-result)) => (contains {:user-id user-id
                                                        :objective-id objective-id
                                                        :invitation-id invitation-id})))

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
