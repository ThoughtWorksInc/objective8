(ns objective8.unit.storage.mappings-test
  (:refer-clojure :exclude [comment])
  (:require [midje.sweet :refer :all]
            [objective8.storage.mappings :refer :all]))

(defn json-type? [thing]
    (and (= (type thing) org.postgresql.util.PGobject)
         (= (.getType thing) "json")))

(defn has-postgres-type? [the-type]
  (fn [thing]
    (and (= (type thing) org.postgresql.util.PGobject)
         (= (.getType thing) the-type))))

(defn time-type? [thing]
  (= (type thing) java.sql.Timestamp))

(fact "Clojure maps are turned into Postgres JSON types"
      (let [transformed-map (map->json-type {:is "a map" :has "some keys"})]
        transformed-map => json-type?
        (.getValue transformed-map) => "{\"is\":\"a map\",\"has\":\"some keys\"}"))

;; OBJECTIVE
(def USER_ID 1234)
(def GLOBAL_ID 3)

(def objective-data {:created-by-id 1
                     :global-id GLOBAL_ID
                     :end-date "2015-01-01T00:01:01Z"})
(facts "About map->objective"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [objective (map->objective objective-data)]
               objective => (contains {:created_by_id 1
                                       :global_id GLOBAL_ID
                                       :end_date  time-type?
                                       :objective json-type?})
               (str (:end_date objective)) => (contains "2015-01-01 00:01")))
       (fact "throws exception if :created-by-id, :end-date, or :global-id are missing"
             (map->objective (dissoc objective-data :created-by-id)) => (throws Exception "Could not transform map to objective")
             (map->objective (dissoc objective-data :end-date)) => (throws Exception "Could not transform map to objective")
             (map->objective (dissoc objective-data :global-id)) => (throws Exception "Could not transform map to objective")))

;;USER
(facts "About map->user"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [user (map->user {:twitter-id "twitter-TWITTERID" :username "username"})]
               user => (contains {:twitter_id "twitter-TWITTERID"
                                  :username "username"
                                  :user_data json-type?})))
       (fact "throws exception if :twitter-id or :username is missing"
                    (map->user {:a "B"}) => (throws Exception "Could not transform map to user")
                    (map->user {:twitter-id "twitter-TWITTERID"}) => (throws Exception "Could not transform map to user")
                    (map->user {:username "username"}) => (throws Exception "Could not transform map to user")))

;;UP-DOWN-VOTES
(def vote-data {:global-id GLOBAL_ID :created-by-id USER_ID :vote-type :up})
(facts "About map->up-down-vote"
       (tabular
        (fact "Column values are pulled out and converted"
              (let [up-down-vote (map->up-down-vote {:global-id GLOBAL_ID :created-by-id USER_ID :vote-type ?vote-type})]
                up-down-vote => (contains {:global_id GLOBAL_ID
                                           :created_by_id USER_ID
                                           :vote ?vote})))
        ?vote-type ?vote
        :up        1
        :down      -1)

       (fact "throws exception if any field is missing"
             (map->up-down-vote (dissoc vote-data :global-id)) => (throws Exception)
             (map->up-down-vote (dissoc vote-data :created-by-id)) => (throws Exception)
             (map->up-down-vote (dissoc vote-data :vote-type)) => (throws Exception))

       (fact "throws exception if vote type is invalid"
             (map->up-down-vote (assoc vote-data :vote-type :not-up-or-down)) => (throws Exception)))

;;COMMENT
(def OBJECTIVE_ID 2345)

(def comment-map {:created-by-id USER_ID
                  :objective-id OBJECTIVE_ID
                  :comment-on-id GLOBAL_ID})

(facts "About map->comment"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [test-comment (map->comment comment-map)]
              test-comment => (contains {:created_by_id USER_ID
                                         :objective_id OBJECTIVE_ID
                                         :comment_on_id GLOBAL_ID
                                         :comment json-type?})))
       (fact "throws exception if :created-by-id, :objective-id or :comment-on-id are missing"
                    (map->comment (dissoc comment-map :created-by-id)) => (throws Exception "Could not transform map to comment")
                    (map->comment (dissoc comment-map :objective-id)) => (throws Exception "Could not transform map to comment")
                    (map->comment (dissoc comment-map :comment-on-id)) => (throws Exception "Could not transform map to comment")))


;;QUESTIONS
(def question-map {:created-by-id USER_ID
                   :objective-id OBJECTIVE_ID})

(facts "About map->question"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [test-question (map->question question-map)]
              test-question => (contains {:created_by_id USER_ID
                                          :objective_id OBJECTIVE_ID
                                          :question json-type?})))
       (fact "throws exception if :created-by-id or :objective-id are missing"
                    (map->question (dissoc question-map :created-by-id)) => (throws Exception "Could not transform map to question")
                    (map->question (dissoc question-map :objective-id)) => (throws Exception "Could not transform map to question")))

;;ANSWERS
(def QUESTION_ID 345)

(def answer-map {:created-by-id USER_ID
                 :question-id QUESTION_ID
                 :objective-id OBJECTIVE_ID
                 :global-id GLOBAL_ID})

(facts "About map->answer"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [test-answer (map->answer answer-map)]
               test-answer => (contains {:created_by_id USER_ID
                                         :question_id QUESTION_ID
                                         :objective_id OBJECTIVE_ID
                                         :global_id GLOBAL_ID
                                         :answer json-type?})))
       (fact "throws exception if :created-by-id or :question-id are missing"
             (map->answer (dissoc answer-map :created-by-id)) => (throws Exception)
             (map->answer (dissoc answer-map :question-id)) => (throws Exception)
             (map->answer (dissoc answer-map :objective-id)) => (throws Exception)
             (map->answer (dissoc answer-map :global-id)) => (throws Exception)))

;;INVITATIONS
(def INVITATION_STATUS "active")

(def invitation-map {:invited-by-id USER_ID
                     :objective-id OBJECTIVE_ID
                     :status INVITATION_STATUS
                     :writer-name "barry"
                     :reason "the reason"
                     :uuid "something-random"})

(facts "About map->invitation"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (map->invitation invitation-map) => (contains {:objective_id OBJECTIVE_ID
                                                            :invited_by_id USER_ID
                                                            :uuid "something-random"
                                                            :status (has-postgres-type? "invitation_status")
                                                            :invitation json-type?}))

       (fact "throws exception if :objective-id, :invited-by-id, :uuid or :status are missing"
             (map->invitation (dissoc invitation-map :objective-id)) => (throws Exception "Could not transform map to invitation")
             (map->invitation (dissoc invitation-map :uuid)) => (throws Exception "Could not transform map to invitation")
             (map->invitation (dissoc invitation-map :invited-by-id)) => (throws Exception "Could not transform map to invitation")
             (map->invitation (dissoc invitation-map :status)) => (throws Exception "Could not transform map to invitation")))

;;CANDIDATES
(def INVITATION_ID 345)

(def candidate-map {:user-id USER_ID
                    :objective-id OBJECTIVE_ID
                    :invitation-id INVITATION_ID})

(facts "About map->candidate"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (map->candidate candidate-map) =>  (contains {:user_id USER_ID
                                                           :objective_id OBJECTIVE_ID
                                                           :invitation_id INVITATION_ID})))

;;DRAFTS
(def draft-map {:submitter-id USER_ID
                :objective-id OBJECTIVE_ID
                :content "Some content"})

(facts "About map->draft"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (map->draft draft-map) => (contains {:submitter_id USER_ID
                                                  :objective_id OBJECTIVE_ID
                                                  :draft json-type?})))

;;BEARER-TOKENS
(def BEARER_NAME "bearer name")
(def BEARER_TOKEN "123")
(def bearer-token-map {:bearer-name BEARER_NAME
                       :bearer-token BEARER_TOKEN
                       :authoriser true})

(facts "About map->bearer-token"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [test-bearer-token (map->bearer-token bearer-token-map)]
               test-bearer-token => (contains {:bearer_name BEARER_NAME
                                               :token_details json-type?})))
       (fact "throws exception if :bearer-name is missing"
             (map->bearer-token (dissoc bearer-token-map
                                        :bearer-name)) => (throws Exception "Could not transform map to bearer-token")))
