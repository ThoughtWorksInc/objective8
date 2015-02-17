(ns objective8.api.questions-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration-helpers :as helpers]
            [objective8.objectives :as objectives]
            [objective8.questions :as questions]))

;; Testing from http request -> making correct calls within questions namespace
;; Mock or stub out 'questions' namespace

(def app (helpers/test-context))

(def OBJECTIVE_ID 234)
(def WRONG_OBJECTIVE_ID (+ OBJECTIVE_ID 1))
(def USER_ID 1)
(def QUESTION_ID 42)

(def the-question {:question "The meaning of life?"
                   :objective-id OBJECTIVE_ID
                   :created-by-id USER_ID})

(def the-invalid-question {:question "The meaning of life?"
                           :created-by-id USER_ID})

(def stored-question (assoc the-question :_id QUESTION_ID))

(def the-question-as-json (str "{\"question\":\"The meaning of life?\",\"objective-id\":" OBJECTIVE_ID ",\"created-by-id\":" USER_ID "}"))

(facts "about posting questions" :integration
       (fact "the posted question is stored"
             (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions")
                        :request-method :post
                        :content-type "application/json"
                        :body the-question-as-json)

             => (helpers/check-json-body stored-question)
             (provided
               (questions/store-question! the-question) => stored-question))

       (fact "the http response indicates the location of the question"
             (against-background
               (questions/store-question! anything) => stored-question)

             (let [result (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions")
                                     :request-method :post
                                     :content-type "application/json"
                                     :body the-question-as-json)
                   response (:response result)
                   headers (:headers response)]
               response => (contains {:status 201})
               headers => (contains {"Location" (contains (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))})))

       (fact "a 400 status is returned if a PSQLException is raised"
          (against-background
               (questions/store-question! anything) =throws=> (org.postgresql.util.PSQLException. 
                                                              (org.postgresql.util.ServerErrorMessage. "" 0)) )
               (:response (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions")
                                     :request-method :post
                                     :content-type "application/json"
                                     :body the-question-as-json)) => (contains {:status 400}))

       (fact "a 400 status is returned if a map->question exception is raised"
             (:response (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions")
                                   :request-method :post
                                   :content-type "application/json"
                                   :body (json/generate-string the-invalid-question))) => (contains {:status 400})))


(facts "about retrieving questions"
       (fact "can retrieve a question using its id"
             (let [peridot-response (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))]
               peridot-response) => (helpers/check-json-body stored-question)
             (provided
               (questions/retrieve-question QUESTION_ID) => stored-question))

      (fact "a 400 status is returned if the question does not belong to the objective"
             (:response  (p/request app (str "/api/v1/objectives/" WRONG_OBJECTIVE_ID 
                                             "/questions/" QUESTION_ID))) 
             => (contains {:status 400}) 
             (provided (questions/retrieve-question QUESTION_ID) => {:objective-id OBJECTIVE_ID
                                                                     :question "The question?"}))) 
