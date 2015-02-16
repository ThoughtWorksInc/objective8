(ns objective8.api.questions-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration-helpers :as helpers]
            [objective8.objectives :as objectives]
            [objective8.questions :as questions]))

;; Testing from http request -> making correct calls within questions namespace
;; Mock or stub out 'questions' namespace

(def app (helpers/test-context))

(def OBJECTIVE_ID 234)
(def USER_ID 1)
(def QUESTION_ID 42)

(def the-question {:question "The meaning of life?"
                   :objective-id OBJECTIVE_ID
                   :created-by-id USER_ID})

(def stored-question (assoc the-question :_id QUESTION_ID))

(def the-question-as-json (str "{\"question\":\"The meaning of life?\",\"objective-id\":" OBJECTIVE_ID ",\"created-by-id\":" USER_ID "}"))

(facts "about posting questions" :integration
       (fact "the posted question is stored"
             (against-background
              (objectives/retrieve-objective OBJECTIVE_ID) => {})
             (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions")
                        :request-method :post
                        :content-type "application/json"
                        :body the-question-as-json)

             => (helpers/check-json-body stored-question)
             (provided
               (questions/store-question! the-question) => stored-question))

       (fact "the http response indicates the location of the question"
             (against-background
               (objectives/retrieve-objective OBJECTIVE_ID) => {}
               (questions/store-question! anything) => stored-question)

             (let [result (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions")
                                     :request-method :post
                                     :content-type "application/json"
                                     :body the-question-as-json)
                   response (:response result)
                   headers (:headers response)]
               response => (contains {:status 201})
               headers => (contains {"Location" (contains (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))}))))

(fact "can retrieve a question using its id"
             (let [peridot-response (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))]
               peridot-response) => (helpers/check-json-body stored-question)
             (provided
               (questions/retrieve-question QUESTION_ID) => stored-question))
