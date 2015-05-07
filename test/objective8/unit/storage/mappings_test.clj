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

(def ANSWER_ID 243)

;; OBJECTIVE
(def USER_ID 1234)
(def GLOBAL_ID 3)

(def objective-data {:created-by-id 1
                     :global-id GLOBAL_ID
                     :status "open"
                     :removed-by-admin false
                     :end-date "2015-01-01T00:01:01Z"
                     :description "Objective description"})

(facts "About map->objective"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [objective (map->objective objective-data)]
               objective => (contains {:created_by_id 1
                                       :global_id GLOBAL_ID
                                       :status (has-postgres-type? "objective_status") 
                                       :removed_by_admin false
                                       :end_date  time-type?
                                       :objective json-type?})
               (str (:end_date objective)) => (contains "2015-01-01 00:01")
               (json-type->map (:objective objective)) =not=> (contains {:global-id anything})
               (json-type->map (:objective objective)) =not=> (contains {:end-date anything})
               (json-type->map (:objective objective)) =not=> (contains {:created-by-id anything})
               (json-type->map (:objective objective)) =not=> (contains {:status anything})
               (json-type->map (:objective objective)) => (contains {:description "Objective description"})))

       (fact "throws exception if :created-by-id, status, removed-by-admin, :end-date, or :global-id are missing"

             (map->objective (dissoc objective-data :created-by-id)) => (throws Exception)
             (map->objective (dissoc objective-data :status)) => (throws Exception)
             (map->objective (dissoc objective-data :removed-by-admin)) => (throws Exception)
             (map->objective (dissoc objective-data :end-date)) => (throws Exception)
             (map->objective (dissoc objective-data :global-id)) => (throws Exception)))

;;USER
(facts "About map->user"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [user (map->user {:twitter-id "twitter-TWITTERID" :username "username"})]
               user => (contains {:twitter_id "twitter-TWITTERID"
                                  :username "username"
                                  :user_data json-type?})
               (json-type->map (:user_data user)) =not=> (contains {:twitter-id anything})
               (json-type->map (:user_data user)) =not=> (contains {:username anything})))
       
       (fact "throws exception if :twitter-id or :username is missing"
             (map->user {:a "B"}) => (throws Exception)
             (map->user {:twitter-id "twitter-TWITTERID"}) => (throws Exception)
             (map->user {:username "username"}) => (throws Exception)))

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

;;COMMENTS
(def OBJECTIVE_ID 2345)

(def comment-map {:global-id GLOBAL_ID
                  :created-by-id USER_ID
                  :objective-id OBJECTIVE_ID
                  :comment-on-id GLOBAL_ID})

(facts "About map->comment"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [test-comment (map->comment comment-map)]
              test-comment => (contains {:global_id GLOBAL_ID
                                         :created_by_id USER_ID
                                         :objective_id OBJECTIVE_ID
                                         :comment_on_id GLOBAL_ID
                                         :comment json-type?})
              (json-type->map (:comment test-comment)) =not=> (contains {:global-id anything})
              (json-type->map (:comment test-comment)) =not=> (contains {:comment-on-id anything})
              (json-type->map (:comment test-comment)) =not=> (contains {:objective-id anything})
              (json-type->map (:comment test-comment)) =not=> (contains {:created-by-id anything})))
       
       (fact "throws exception if :global-id, :created-by-id, :objective-id or :comment-on-id are missing"
             (map->comment (dissoc comment-map :global-id)) => (throws Exception)
             (map->comment (dissoc comment-map :created-by-id)) => (throws Exception)
             (map->comment (dissoc comment-map :objective-id)) => (throws Exception)
             (map->comment (dissoc comment-map :comment-on-id)) => (throws Exception)))


;;QUESTIONS
(def question-map {:created-by-id USER_ID
                   :objective-id OBJECTIVE_ID})

(facts "About map->question"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [test-question (map->question question-map)]
              test-question => (contains {:created_by_id USER_ID
                                          :objective_id OBJECTIVE_ID
                                          :question json-type?})
              (json-type->map (:question test-question)) =not=> (contains {:created-by-id anything})
              (json-type->map (:question test-question)) =not=> (contains {:objective-id anything})))
       
       (fact "throws exception if :created-by-id or :objective-id are missing"
             (map->question (dissoc question-map :created-by-id)) => (throws Exception)
             (map->question (dissoc question-map :objective-id)) => (throws Exception)))

;;MARKS
(def QUESTION_ID 324)

(def mark-map {:created-by-id USER_ID
               :question-id QUESTION_ID
               :active true})

(facts "About map->mark"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [test-mark (map->mark mark-map)]
               test-mark => {:created_by_id USER_ID
                             :question_id QUESTION_ID
                             :active true}))

       (fact "throws exception if :created-by-id, :question-id or :active are missing"
             (map->mark (dissoc mark-map :created-by-id)) => (throws Exception)
             (map->mark (dissoc mark-map :question-id)) => (throws Exception)
             (map->mark (dissoc mark-map :active)) => (throws Exception)))

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
                                         :answer json-type?})
               (json-type->map (:answer test-answer)) =not=> (contains {:question-id anything})
               (json-type->map (:answer test-answer)) =not=> (contains {:created-by-id anything})
               (json-type->map (:answer test-answer)) =not=> (contains {:global-id anything})
               (json-type->map (:answer test-answer)) =not=> (contains {:objective-id anything})))

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
             (let [invitation (map->invitation invitation-map)]
               invitation => (contains {:objective_id OBJECTIVE_ID
                                        :invited_by_id USER_ID
                                        :uuid "something-random"
                                        :status (has-postgres-type? "invitation_status")
                                        :invitation json-type?})
               (json-type->map (:invitation invitation)) =not=> (contains {:uuid anything})
               (json-type->map (:invitation invitation)) =not=> (contains {:invited-by-id anything})
               (json-type->map (:invitation invitation)) =not=> (contains {:objective-id anything})
               (json-type->map (:invitation invitation)) =not=> (contains {:status anything})))

       (fact "throws exception if :objective-id, :invited-by-id, :uuid or :status are missing"
             (map->invitation (dissoc invitation-map :objective-id)) => (throws Exception)
             (map->invitation (dissoc invitation-map :uuid)) => (throws Exception)
             (map->invitation (dissoc invitation-map :invited-by-id)) => (throws Exception)
             (map->invitation (dissoc invitation-map :status)) => (throws Exception)))

;;WRITERS
(def INVITATION_ID 345)

(def writer-map {:user-id USER_ID
                 :objective-id OBJECTIVE_ID
                 :invitation-id INVITATION_ID})

(facts "About map->writer"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [writer (map->writer writer-map)]
               writer => (contains {:user_id USER_ID
                                    :objective_id OBJECTIVE_ID
                                    :invitation_id INVITATION_ID
                                    :writer json-type?})
               (json-type->map (:writer writer)) =not=> (contains {:objective-id anything})
               (json-type->map (:writer writer)) =not=> (contains {:user-id anything})
               (json-type->map (:writer writer)) =not=> (contains {:invitation-id anything}))))

;;WRITER-NOTES
(def writer-note-map {:global-id GLOBAL_ID
                      :objective-id OBJECTIVE_ID
                      :created-by-id USER_ID
                      :note-on-id ANSWER_ID})

(facts "About map->writer-note"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [writer-note (map->writer-note writer-note-map)]
               writer-note => (contains {:global_id GLOBAL_ID
                                         :created_by_id USER_ID
                                         :note_on_id ANSWER_ID
                                         :note json-type?
                                         :objective_id OBJECTIVE_ID})
               (json-type->map (:note writer-note)) =not=> (contains {:created-by-id anything})
               (json-type->map (:note writer-note)) =not=> (contains {:note-on-id anything})
               (json-type->map (:note writer-note)) =not=> (contains {:global-id anything})
               (json-type->map (:note writer-note)) =not=> (contains {:objective-id anything})))


       (fact "throws exception if :created-by-id, :note-on-id or :objective-id are missing"

             (map->writer-note (dissoc writer-note-map :global-id)) => (throws Exception)
             (map->writer-note (dissoc writer-note-map :created-by-id)) => (throws Exception)
             (map->writer-note (dissoc writer-note-map :note-on-id)) => (throws Exception)
             (map->writer-note (dissoc writer-note-map :objective-id)) => (throws Exception))) 


;;DRAFTS
(def draft-map {:submitter-id USER_ID
                :objective-id OBJECTIVE_ID
                :global-id GLOBAL_ID
                :content "Some content"})

(facts "About map->draft"
       (fact "Column values are pulled out and converted, the map gets turned to json"
             (let [draft (map->draft draft-map)]
               draft => (contains {:submitter_id USER_ID
                                   :objective_id OBJECTIVE_ID
                                   :global_id GLOBAL_ID
                                   :draft json-type?})
               (json-type->map (:draft draft)) =not=> (contains {:submitter-id anything})
               (json-type->map (:draft draft)) =not=> (contains {:global-id anything})
               (json-type->map (:draft draft)) =not=> (contains {:objective-id anything}))))

;;SECTIONS
(def SECTION_LABEL "abcd1234")
(def DRAFT_ID 2)
(def section-map {:section-label SECTION_LABEL
                  :draft-id DRAFT_ID
                  :objective-id OBJECTIVE_ID
                  :global-id GLOBAL_ID})

(facts "About map->section"
       (fact "Column values are pulled out and converted"
             (map->section section-map) => {:global_id GLOBAL_ID
                                            :draft_id DRAFT_ID
                                            :objective_id OBJECTIVE_ID
                                            :section_label SECTION_LABEL})
       
       (fact "throws exception if :global-id, :draft-id or :section-label are missing"
             (map->section (dissoc section-map :global-id)) => (throws Exception)
             (map->section (dissoc section-map :draft-id)) => (throws Exception)
             (map->section (dissoc section-map :objective-id)) => (throws Exception)
             (map->section (dissoc section-map :section-label)) => (throws Exception)))

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
                                               :token_details json-type?})
               (json-type->map (:token_details test-bearer-token)) =not=> (contains {:bearer-name anything})))
       (fact "throws exception if :bearer-name is missing"
             (map->bearer-token (dissoc bearer-token-map
                                        :bearer-name)) => (throws Exception)))

(def star-map {:objective-id OBJECTIVE_ID
               :created-by-id USER_ID
               :active true})

(facts "About map->star"
       (fact "Column values are pulled out and converted"
             (let [star (map->star star-map)]
               star => (contains {:objective_id OBJECTIVE_ID
                                  :created_by_id USER_ID
                                  :active true})))

       (fact "throws exception if :objective-id, :created-by-id or :active are missing"
             (map->star (dissoc star-map :objective-id)) => (throws Exception)
             (map->star (dissoc star-map :created-by-id)) => (throws Exception)
             (map->star (dissoc star-map :active)) => (throws Exception)))

(def OBJECTIVE_URI (str "/objectives/" OBJECTIVE_ID))
(def admin-removal-map {:removed-by-id USER_ID
                        :removal-uri OBJECTIVE_URI})

(facts "About map->admin-removal"
       (fact "Column vales are pulled out and coverted"
             (map->admin-removal admin-removal-map) => {:removed_by_id USER_ID
                                                        :removal_uri OBJECTIVE_URI})

       (fact "throws exception if :removed-by-id or :removal-uri are missing"
             (map->admin-removal (dissoc admin-removal-map :removed-by-id)) => (throws Exception)
             (map->admin-removal (dissoc admin-removal-map :removal-uri)) => (throws Exception)))

;; db-insertion-mapper
(facts "about preparing maps for insertion into the db"
       (fact "should convert clojure keys to db columns"
             (let [map->db-object (db-insertion-mapper "test-entity" nil [:a-key])]
               (map->db-object {:a-key 1}) => {:a_key 1}))

       (fact "should apply transformations to data"
             (let [map->db-object (db-insertion-mapper "test-entity" nil [:a :b] {:a inc})]
               (map->db-object {:a 1 :b 1}) => {:a 2 :b 1}))

       (fact "should include json if required"
             (let [map->db-object (db-insertion-mapper "test-entity" :json-key [:a])]
               (map->db-object {:a 1 :other "data"}) => {:a 1
                                                         :json_key (map->json-type {:other "data"})}))

       (fact "should throw an exception if required keys are missing"
             (let [map->db-object (db-insertion-mapper "test-entity" nil [:a])]
               (map->db-object {}) => (throws Exception))))

;; Unmapping
(def unmap-nothing (constantly {}))

(facts "about unmapping columns"
       (fact "should extract values from a column key into a clojure key"
             (let [unmapper (-> unmap-nothing (with-columns [:a-key :b-key]))]
               (unmapper {:a_key 1 :b_key 2 :c_key 3}) => {:a-key 1 :b-key 2}))

       (fact "should apply any required transformations to the data"
             (let [unmapper (-> unmap-nothing (with-columns [:a-key :b-key] {:a-key inc :b-key dec}))]
               (unmapper {:a_key 1 :b_key 2}) => {:a-key 2 :b-key 1})))
