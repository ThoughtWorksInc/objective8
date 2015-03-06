(ns objective8.api.answers-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration-helpers :as helpers]
            [objective8.storage-helpers :as sh]
            [objective8.objectives :as objectives]
            [objective8.questions :as questions]
            [objective8.middleware :as m]
            [objective8.answers :as answers]))

(def app (helpers/test-context))

(def OBJECTIVE_ID 234)
(def WRONG_OBJECTIVE_ID (+ OBJECTIVE_ID 1))
(def USER_ID 1)
(def QUESTION_ID 42)
(def ANSWER_ID 3)

(def INVALID_ID "NOT_AN_INTEGER")

(defn an-answer [objective-id question-id created-by-id]
  {:answer "The answer is 42"
   :question-id question-id
   :objective-id objective-id
   :created-by-id created-by-id})

(def the-answer (an-answer OBJECTIVE_ID QUESTION_ID USER_ID))
(def the-invalid-answer (dissoc the-answer :question-id))

(facts "POST /api/v1/objectives/:id/questions/:id/answers" :integration
       (against-background
        (m/valid-credentials? anything anything anything) => true)
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "creates an answer"
              (let [{q-id :_id obj-id :objective-id created-by-id :created-by-id} (sh/store-a-question)
                    answer (an-answer obj-id q-id created-by-id)
                    {response :response} (p/request app (str "/api/v1/objectives/" obj-id "/questions/" q-id "/answers")
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string answer))]
                (:body response) => (helpers/json-contains (assoc answer :_id integer?))
                (:headers response) => (helpers/location-contains (str "api/v1/objectives/" obj-id "/questions/" q-id "/answers/"))
                (:status response) => 201)) 

        (fact "a 423 (resource locked) status is returned when drafting has started on the objective"
              (let [{obj-id :_id :as objective} (sh/store-an-objective-in-draft)
                    {q-id :_id created-by-id :created-by-id} (sh/store-a-question {:objective objective})
                    answer (an-answer obj-id q-id created-by-id)
                    {response :response} (p/request app (str "/api/v1/objectives/" obj-id "/questions/" q-id "/answers")
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string answer))]
                (:status response) => 423))

        (fact "a 400 status is returned if a PSQLException is raised"
              (against-background
               (answers/store-answer! anything) =throws=> (org.postgresql.util.PSQLException.
                                                           (org.postgresql.util.ServerErrorMessage. "" 0)))
              (get-in (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers")
                                 :request-method :post
                                 :content-type "application/json"
                                 :body (json/generate-string the-answer))
                      [:response :status]) => 400)

        (fact "a 400 status is returned if a map->answer exception is raised"
              (get-in (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers")
                                 :request-method :post
                                 :content-type "application/json"
                                 :body (json/generate-string the-invalid-answer))
                      [:response :status]) => 400)

        (tabular
         (fact "a 400 status is returned if the objective and question ids are not integers"
               (get-in (p/request app (str "/api/v1/objectives/" ?objective_id 
                                           "/questions/" ?question_id "/answers")
                                  :request-method :post
                                  :content-type "application/json"
                                  :body (json/generate-string the-answer))
                       [:response :status]) => 400)
         ?objective_id  ?question_id
         INVALID_ID     QUESTION_ID
         OBJECTIVE_ID   INVALID_ID
         INVALID_ID     INVALID_ID)

        (fact "a 400 status is returned if the question does not belong to the objective"
              (get-in (p/request app (str "/api/v1/objectives/" WRONG_OBJECTIVE_ID 
                                          "/questions/" QUESTION_ID "/answers")
                                 :request-method :post
                                 :content-type "application/json"
                                 :body "")
                      [:response :status]) => 400
                      (provided (questions/retrieve-question QUESTION_ID) => {:objective-id OBJECTIVE_ID
                                                                              :question "The question?"}))))

(def stored-answers (map #(assoc the-answer :_id %) (range 5)))

(facts "about retrieving answers"
       (fact "answers can be retrieved for a question"
             (against-background 
              (questions/retrieve-question QUESTION_ID) => {:objective-id OBJECTIVE_ID})
             (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers"))
             => (helpers/check-json-body stored-answers)
             (provided
              (answers/retrieve-answers QUESTION_ID) => stored-answers))

       (fact "a 400 status is returned if the question does not belong to the objective"
             (:response  (p/request app (str "/api/v1/objectives/" WRONG_OBJECTIVE_ID 
                                             "/questions/" QUESTION_ID "/answers")))
             => (contains {:status 400}) 
             (provided (questions/retrieve-question QUESTION_ID) => {:objective-id OBJECTIVE_ID
                                                                     :question "The question?"}))) 
