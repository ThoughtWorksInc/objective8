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
(def USER_ID 1)
(def QUESTION_ID 42)
(def ANSWER_ID 3)

(def the-answer {:answer "The answer is 42"
                 :question-id QUESTION_ID
                 :created-by-id USER_ID })

(def stored-answer (assoc the-answer :_id ANSWER_ID))

(facts "about posting answers" :integation
       (fact "the posted answer is stored"
             (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers")
                        :request-method :post
                        :content-type "application/json"
                        :body (json/generate-string the-answer))

             => (helpers/check-json-body stored-answer)
             (provided
               (answers/store-answer! the-answer) => stored-answer)

      (fact "the http response indicates the location of the answer"
            (against-background
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
                                                              "/answers/" ANSWER_ID))})))))
