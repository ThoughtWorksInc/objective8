(ns objective8.integration.api.questions
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.objectives :as objectives]
            [objective8.middleware :as m]
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
(def stored-questions [stored-question])

(defn a-question
  ([objective-id user-id] {:question "The meaning of life is?"
                           :objective-id objective-id
                           :created-by-id user-id})
  ([] (a-question OBJECTIVE_ID USER_ID)))

(defn the-question-as-json
  ([objective-id user-id] (json/generate-string (a-question objective-id user-id)))
  ([] (the-question-as-json OBJECTIVE_ID USER_ID)))

(facts "about posting questions"
       (against-background
         (m/valid-credentials? anything anything anything) => true)
       (against-background
         [(before :contents (do
                              (helpers/db-connection)
                              (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "the posted question is stored, and the resource location is reported"
               (let [{obj-id :_id user-id :created-by-id} (sh/store-an-open-objective)
                     question (a-question obj-id user-id)
                     {response :response} (p/request app (str "/api/v1/objectives/" obj-id "/questions")
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body (json/generate-string question))]
                 (:body response) => (helpers/json-contains (assoc question :_id integer?))
                 (:status response) => 201
                 (:headers response) => (helpers/location-contains (str "/api/v1/objectives/" obj-id
                                                                        "/questions/"))))

         (fact "a 400 status is returned if a PSQLException is raised"
               (against-background
                 (questions/store-question! anything) =throws=> (org.postgresql.util.PSQLException.
                                                                  (org.postgresql.util.ServerErrorMessage. "" 0)))
               (let [{obj-id :_id} (sh/store-an-open-objective)]
                 (:response (p/request app (str "/api/v1/objectives/" obj-id "/questions")
                                       :request-method :post
                                       :content-type "application/json"
                                       :body (the-question-as-json)))) => (contains {:status 400}))

         (fact "a 400 status is returned if a map->question exception is raised"
               (let [{obj-id :_id} (sh/store-an-open-objective)]
                 (:response (p/request app (str "/api/v1/objectives/" obj-id "/questions")
                                       :request-method :post
                                       :content-type "application/json"
                                       :body (json/generate-string the-invalid-question)))) => (contains {:status 400}))

         (fact "a 423 (locked) status is returned when trying to post a question to an objective that has status=drafting"
               (let [{objective-id :_id user-id :created-by-id} (sh/store-an-objective-in-draft)]
                 (:response (p/request app (str "/api/v1/objectives/" objective-id "/questions")
                                       :request-method :post
                                       :content-type "application/json"
                                       :body (the-question-as-json objective-id user-id))) => (contains {:status 423})))))


(facts "about retrieving questions"
       (fact "can retrieve a question using its id"
             (let [peridot-response (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))]
               peridot-response) => (helpers/check-json-body stored-question)
             (provided
               (questions/retrieve-question QUESTION_ID) => stored-question))

       (fact "a 404 status is returned if the question does not belong to the objective"
             (:response  (p/request app (str "/api/v1/objectives/" WRONG_OBJECTIVE_ID 
                                             "/questions/" QUESTION_ID))) 
             => (contains {:status 404}) 
             (provided (questions/retrieve-question QUESTION_ID) => {:objective-id OBJECTIVE_ID
                                                                     :question "The question?"}))

       (fact "questions can be retrieved for an objective"
             (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions"))
             => (helpers/check-json-body stored-questions)
             (provided
               (questions/retrieve-questions OBJECTIVE_ID) => stored-questions))) 
