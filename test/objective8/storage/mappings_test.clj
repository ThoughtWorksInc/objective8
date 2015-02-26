(ns objective8.storage.mappings-test
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
(facts "About map->objective"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [objective (map->objective {:created-by-id 1
                                              :end-date "2015-01-01T00:01:01Z"})]
               objective => (contains {:created_by_id 1
                                       :end_date  time-type?
                                       :objective json-type?})
               (str (:end_date objective)) => (contains "2015-01-01 00:01")))
       (fact "throws exception if :created-by-id or :end-date are missing"
             (map->objective {:a "B"}) => (throws Exception "Could not transform map to objective")
             (map->objective {:a "B" :created-by "Foo"}) => (throws Exception "Could not transform map to objective")
             (map->objective {:a "B" :end-date "Blah"}) => (throws Exception "Could not transform map to objective")))

;;USER
(facts "About map->user"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [user (map->user {:twitter-id "twitter-TWITTERID"})]
               user => (contains {:twitter_id "twitter-TWITTERID"
                                  :user_data json-type?})))
       (fact "throws exception if :twitter-id is missing"
                    (map->user {:a "B"}) => (throws Exception "Could not transform map to user")))

;;COMMENT
(def USER_ID 1234)
(def OBJECTIVE_ID 2345)

(def comment-map {:created-by-id USER_ID
                  :objective-id OBJECTIVE_ID})

(facts "About map->comment"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [test-comment (map->comment comment-map)]
              test-comment => (contains {:created_by_id USER_ID
                                         :objective_id OBJECTIVE_ID
                                         :comment json-type?})))
       (fact "throws exception if :created-by-id or :objective-id are missing"
                    (map->comment (dissoc comment-map :created-by-id)) => (throws Exception "Could not transform map to comment")
                    (map->comment (dissoc comment-map :objective-id)) => (throws Exception "Could not transform map to comment")))


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
                 :question-id QUESTION_ID})

(facts "About map->answer"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [test-answer (map->answer answer-map)]
               test-answer => (contains {:created_by_id USER_ID
                                         :question_id QUESTION_ID  
                                         :answer json-type?})))
       (fact "throws exception if :created-by-id or :question-id are missing"
                    (map->answer (dissoc answer-map :created-by-id)) => (throws Exception "Could not transform map to answer")
                    (map->answer (dissoc answer-map :question-id)) => (throws Exception "Could not transform map to answer")))

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
             (let [test-invitation (map->invitation invitation-map)]
               test-invitation => (contains {:objective_id OBJECTIVE_ID
                                             :invited_by_id USER_ID
                                             :uuid "something-random"
                                             :status (has-postgres-type? "invitation_status") 
                                             :invitation json-type?})))

       (fact "throws exception if :objective-id, :invited-by-id, :uuid or :status are missing"
             (map->invitation (dissoc invitation-map :objective-id)) => (throws Exception "Could not transform map to invitation")
             (map->invitation (dissoc invitation-map :uuid)) => (throws Exception "Could not transform map to invitation")
             (map->invitation (dissoc invitation-map :invited-by-id)) => (throws Exception "Could not transform map to invitation")
             (map->invitation (dissoc invitation-map :status)) => (throws Exception "Could not transform map to invitation")))




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
