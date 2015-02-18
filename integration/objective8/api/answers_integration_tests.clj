(ns objective8.api.answers-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration-helpers :as helpers]
            [objective8.objectives :as objectives]
            [objective8.questions :as questions]
            [objective8.answers :as answers]))

(def app (helpers/test-context))

(def OBJECTIVE_ID 234)
(def WRONG_OBJECTIVE_ID (+ OBJECTIVE_ID 1))
(def USER_ID 1)
(def QUESTION_ID 42)
(def ANSWER_ID 3)

(def INVALID_ID "NOT_AN_INTEGER")

(def the-answer {:answer "The answer is 42"
                 :question-id QUESTION_ID
                 :objective-id OBJECTIVE_ID
                 :created-by-id USER_ID })

(def the-invalid-answer (dissoc the-answer :question-id))

(def stored-answer (assoc the-answer :_id ANSWER_ID))
(def stored-answers (map #(assoc the-answer :_id %) (range 5)))

(facts "about posting answers" :integation
       (fact "the posted answer is stored"
             (against-background 
               (questions/retrieve-question QUESTION_ID) => {:objective-id OBJECTIVE_ID})
             (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers")
                        :request-method :post
                        :content-type "application/json"
                        :body (json/generate-string the-answer))

             => (helpers/check-json-body stored-answer)
             (provided
               (answers/store-answer! the-answer) => stored-answer)) 

      (fact "the http response indicates the location of the answer"
            (against-background
              (questions/retrieve-question QUESTION_ID) => {:objective-id OBJECTIVE_ID} 
              (answers/store-answer! anything) => stored-answer)
            (let [result (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers")
                                    :request-method :post
                                    :content-type "application/json"
                                    :body (json/generate-string the-answer))
                  response (:response result)
                  headers (:headers response)]
              response => (contains {:status 201})
              headers => (contains {"Location" (contains (str "api/v1/objectives/" OBJECTIVE_ID
                                                              "/questions/" QUESTION_ID
                                                              "/answers/" ANSWER_ID))})))

       (fact "a 400 status is returned if a PSQLException is raised"
             (against-background
               (answers/store-answer! anything) =throws=> (org.postgresql.util.PSQLException.
                                                            (org.postgresql.util.ServerErrorMessage. "" 0)))
             (:response (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers")
                                   :request-method :post
                                   :content-type "application/json"
                                   :body (json/generate-string the-answer))) => (contains {:status 400}))

       (fact "a 400 status is returned if a map->answer exception is raised"
             (:response (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers")
                                   :request-method :post
                                   :content-type "application/json"
                                   :body (json/generate-string the-invalid-answer))) => (contains {:status 400}))

       (tabular
         (fact "a 400 status is returned if the objective and question ids are not integers"
               (against-background
                 (answers/store-answer! anything) => stored-answer)
               (:response (p/request app (str "/api/v1/objectives/" ?objective_id 
                                              "/questions/" ?question_id "/answers")
                                     :request-method :post
                                     :content-type "application/json"
                                     :body (json/generate-string the-answer))) => (contains {:status 400}))
         ?objective_id  ?question_id
         INVALID_ID     QUESTION_ID
         OBJECTIVE_ID   INVALID_ID
         INVALID_ID     INVALID_ID)

       (fact "a 400 status is returned if the question does not belong to the objective"
             (:response (p/request app (str "/api/v1/objectives/" WRONG_OBJECTIVE_ID 
                                            "/questions/" QUESTION_ID "/answers")
                                   :request-method :post
                                   :content-type "application/json"
                                   :body "")) => (contains {:status 400})
             (provided (questions/retrieve-question QUESTION_ID) => {:objective-id OBJECTIVE_ID
                                                                     :question "The question?"})
             ))

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
