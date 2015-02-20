(ns objective8.db.storage-tests
  (:require [midje.sweet :refer :all]
            [objective8.storage.storage :as storage]
            [objective8.storage.database :as db]
            [objective8.integration-helpers :refer [truncate-tables
                                                    db-connection]]))

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
                      (let [user {:entity :user :twitter-id "the-twitter-id" :foo "bar"}
                            store-result (storage/pg-store! user)
                            retrieve-result (storage/pg-retrieve {:entity :user :_id (:_id store-result)})]
                        (first (:result retrieve-result))) => (contains {:twitter-id "the-twitter-id" :foo "bar"}))

                ;;OBJECTIVES
                (fact "an objective entity can be stored in the database"
                      (let [stored-user (storage/pg-store! {:entity :user
                                                            :twitter-id "twitter-TWITTER_ID"})
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
                                                                        :end-date "2015-01-01T00:00:00Z"
                                                                        :title "title"})))
                ;;COMMENTS
                (fact "a comment entity can be stored in the database"
                      (let [stored-user (storage/pg-store! {:entity :user
                                                            :twitter-id "twitter-TWITTER_ID"})
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
                                                                        :objective-id objective-id})))

                (fact "can limit the number of results when retrieving"
                      (let [users [{:entity :user :twitter-id "the-twitter-id1"}
                                   {:entity :user :twitter-id "the-twitter-id2"}
                                   {:entity :user :twitter-id "the-twitter-id3"}]
                            store-result (doall (map storage/pg-store! users))
                            retrieve-result (storage/pg-retrieve {:entity :user} {:limit 2})]
                        (count (:result retrieve-result)) => 2))

                (fact "results are retrieved in chronological order"
                      (let [users [{:entity :user :twitter-id "the-twitter-id1"}
                                   {:entity :user :twitter-id "the-twitter-id2"}
                                   {:entity :user :twitter-id "the-twitter-id3"}]
                            store-result (doall (map storage/pg-store! users))
                            retrieved-users (:result (storage/pg-retrieve {:entity :user}))
                            sorted-twitter-ids (vec (map :twitter-id retrieved-users))]
                        sorted-twitter-ids)
                      => ["the-twitter-id1" "the-twitter-id2" "the-twitter-id3"])

                ;;QUESTIONS
                (fact "a question entity can be stored in the database"
                      (let [stored-user (storage/pg-store! {:entity :user
                                                            :twitter-id "twitter-TWITTER_ID"})
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
                                                                        :objective-id objective-id}))) 

                ;;ANSWERS
                (fact "an answer entity can be stored in the database"
                      (let [stored-user (storage/pg-store! {:entity :user
                                                            :twitter-id "twitter-TWITTER_ID"})
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
                            retrieve-result (storage/pg-retrieve {:entity :answer :_id (:_id store-result)})]
                        (first (:result retrieve-result)) => (contains {:created-by-id user-id
                                                                        :question-id question-id}))) 
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
                 (first (:result retrieve-result)) => (contains {:bearer-token "new-token"}))))))))
