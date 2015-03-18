(ns objective8.integration.api.answers
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.objectives :as objectives]
            [objective8.questions :as questions]
            [objective8.middleware :as m]
            [objective8.answers :as answers]))

(def app (helpers/test-context))

(def OBJECTIVE_ID 234)
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

(facts "POST /api/v1/objectives/:id/questions/:id/answers"
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

        (fact "returns a 423 (resource locked) status when drafting has started on the objective"
              (let [{obj-id :_id :as objective} (sh/store-an-objective-in-draft)
                    {q-id :_id created-by-id :created-by-id} (sh/store-a-question {:objective objective})
                    answer (an-answer obj-id q-id created-by-id)
                    {response :response} (p/request app (str "/api/v1/objectives/" obj-id "/questions/" q-id "/answers")
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string answer))]
                (:status response) => 423))

        (fact "returns a 400 status if a PSQLException is raised"
              (against-background
               (answers/store-answer! anything) =throws=> (org.postgresql.util.PSQLException.
                                                           (org.postgresql.util.ServerErrorMessage. "" 0)))
              (get-in (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers")
                                 :request-method :post
                                 :content-type "application/json"
                                 :body (json/generate-string the-answer))
                      [:response :status]) => 400)

        (fact "returns a 400 status if a map->answer exception is raised"
              (get-in (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers")
                                 :request-method :post
                                 :content-type "application/json"
                                 :body (json/generate-string the-invalid-answer))
                      [:response :status]) => 400)

        (tabular
         (fact "returns an error if the objective and question ids are not integers"
               (get-in (p/request app (str "/api/v1/objectives/" ?objective_id 
                                           "/questions/" ?question_id "/answers")
                                  :request-method :post
                                  :content-type "application/json"
                                  :body (json/generate-string the-answer))
                       [:response :status]) => 404)
         ?objective_id  ?question_id
         INVALID_ID     QUESTION_ID
         OBJECTIVE_ID   INVALID_ID
         INVALID_ID     INVALID_ID)

        (fact "returns a 400 status if the question does not belong to the objective"
              (let [{q-id :_id o-id :objective-id} (sh/store-a-question)
                    {response :response} (p/request app (str "/api/v1/objectives/" (inc o-id) 
                                                             "/questions/" q-id "/answers")
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body "")]
                (:status response) => 400))))

(facts "GET /api/v1/objectives/:id/questions/:id/answers"
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "retrieves answers for a question"
               (let [{objective-id :objective-id q-id :_id :as question} (sh/store-a-question)
                     stored-answers (doall (->> (repeat {:question question})
                                                (take 5)
                                                (map sh/store-an-answer)
                                                (map #(dissoc % :username))))
                     {response :response} (p/request app (str "/api/v1/objectives/" objective-id "/questions/" q-id "/answers"))]
                 (:body response) => (helpers/json-contains (map contains stored-answers))))

         (future-fact "retrieves vote count for each answer"
               (let [{objective-id :objective-id q-id :_id :as question} (sh/store-a-question)
                     {global-id :global-id} (sh/store-an-answer {:question question})
                     up-votes (doall (for [_ (range 5)] (sh/store-an-up-down-vote global-id :up)))
                     down-votes (doall (for [_ (range 3)] (sh/store-an-up-down-vote global-id :down)))
                     {response :response} (p/request app (utils/path-for :api/get-answers-for-question
                                                                         :id objective-id
                                                                         :q-id q-id))]
                 (:body response) => (helpers/json-contains [(contains {:votes {:up 5 :down 3}})])))

         (fact "returns a 400 status if the question does not belong to the objective"
               (let [{q-id :_id o-id :objective-id} (sh/store-a-question)
                     {response :response} (p/request app (str "/api/v1/objectives/" (inc o-id)
                                                              "/questions/" q-id "/answers"))]
                 (:status response) => 400))))
