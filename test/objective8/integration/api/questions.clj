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

(facts "POST /api/v1/objective/:id/questions"
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

         (fact "a 403 (forbidden) status is returned when trying to post a question to an objective that has status=drafting"
               (let [{objective-id :_id user-id :created-by-id} (sh/store-an-objective-in-draft)]
                 (:response (p/request app (str "/api/v1/objectives/" objective-id "/questions")
                                       :request-method :post
                                       :content-type "application/json"
                                       :body (the-question-as-json objective-id user-id))) => (contains {:status 403})))))


(facts "GET /api/v1/objectives/:id/questions/:q-id"
       (against-background
        [(before :contents (do
                             (helpers/db-connection)
                             (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "can retrieve a question using its id"
              (let [{question-id :_id objective-id :objective-id :as question} (sh/store-a-question)
                    {response :response} (p/request app (str "/api/v1/objectives/" objective-id "/questions/" question-id))]
                (:body response) => (helpers/json-contains question)))

        (fact "a 404 status is returned if the question does not belong to the objective"
              (let [{question-id :_id objective-id :objective-id :as question} (sh/store-a-question)
                    wrong-objective-id (inc objective-id)
                    {response :response} (p/request app (str "/api/v1/objectives/" wrong-objective-id "/questions/" question-id))]
                (:status response) => 404))))

(facts "GET /api/v1/objectives/:id/questions"
       (against-background
        [(before :contents (do
                             (helpers/db-connection)
                             (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "questions can be retrieved for an objective"
             (let [{objective-id :_id :as objective} (sh/store-an-open-objective)
                   question-1 (sh/store-a-question {:objective objective})
                   question-2 (sh/store-a-question {:objective objective})
                   {response :response} (p/request app (str "/api/v1/objectives/" objective-id "/questions"))

                   expected-result [question-1 question-2]]
               (:body response) => (helpers/json-contains (map contains expected-result) :in-any-order)))

         (fact "questions for an objective can be retrieved sorted by answer counts"
               (let [{objective-id :_id :as objective} (sh/store-an-open-objective)
                    {one-answer-q-id  :_id :as question-with-one-answer} (sh/store-a-question {:objective objective})
                    {two-answer-q-id :_id :as question-with-two-answers} (sh/store-a-question {:objective objective})]
                    (sh/store-an-answer {:question question-with-one-answer})

                    (sh/store-an-answer {:question question-with-two-answers})
                    (sh/store-an-answer {:question question-with-two-answers})

                 (-> (p/request app (str "/api/v1/objectives/" objective-id "/questions")
                            :params {:sorted-by "answers"})
                     (get-in [:response :body])) => (helpers/json-contains [(contains {:_id two-answer-q-id})
                                                                            (contains {:_id one-answer-q-id})])))

         (fact "marks on questions are included when questions are retrieved for an objective"
               (let [objective (sh/store-an-open-objective)
                     {marked-by :username :as marking-user} (sh/store-a-user)
                     marking-writer (sh/store-a-writer {:invitation (sh/store-an-invitation {:objective objective})
                                                        :user marking-user})

                     marked-question (sh/store-a-question {:objective objective})
                     _ (sh/store-a-mark {:question marked-question :writer marking-writer})

                     unmarked-question (sh/store-a-question {:objective objective})

                     {response :response} (p/request app (utils/path-for :api/get-questions-for-objective :id (:_id objective)))]
                 (:body response) => (helpers/json-contains [(contains {:_id (:_id marked-question)
                                                                        :meta {:marked true
                                                                               :marked-by marked-by}})
                                                             (contains {:_id (:_id unmarked-question)
                                                                        :meta {:marked false}})]
                                                            :in-any-order)))))

(facts "POST /api/v1/meta/marks"
       (against-background
         (m/valid-credentials? anything anything anything) => true)
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "the posted mark is stored"
             (let [{objective-id :_id :as objective} (sh/store-an-open-objective)
                   invitation (sh/store-an-invitation {:objective objective})
                   {question-id :_id} (sh/store-a-question {:objective objective})
                   {user-id :user-id} (sh/store-a-writer {:invitation invitation})

                   question-uri (str "/objectives/" objective-id "/questions/" question-id)
                   created-by-uri (str "/users/" user-id) 

                   mark-data {:question-uri question-uri
                         :created-by-uri created-by-uri}

                   {response :response} (p/request app "/api/v1/meta/marks"
                                                   :request-method :post
                                                   :content-type "application/json"
                                                   :body (json/generate-string mark-data))]
               (:status response) => 201
               (:body response) => (helpers/json-contains {:uri (contains "/meta/marks/")
                                                           :question-uri question-uri
                                                           :created-by-uri created-by-uri
                                                           :active true})
               (:headers response) => (helpers/location-contains "/api/v1/meta/marks/")))

         (fact "posting a mark referring to a marked question toggles the existing mark"
               (let [{objective-id :_id :as objective} (sh/store-an-open-objective)
                     invitation (sh/store-an-invitation {:objective objective})
                     {question-id :_id :as question} (sh/store-a-question {:objective objective})
                     {user-id :user-id} (sh/store-a-writer {:invitation invitation})

                     existing-mark (sh/store-a-mark {:question question})

                     question-uri (str "/objectives/" objective-id "/questions/" question-id)
                     created-by-uri (str "/users/" user-id)

                     new-mark-data {:created-by-uri created-by-uri
                                    :question-uri question-uri}

                     {response :response} (p/request app "/api/v1/meta/marks"
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body (json/generate-string new-mark-data))]
                 (:status response) => 201
                 (:body response) => (helpers/json-contains {:uri (contains "/meta/marks/")
                                                             :question-uri question-uri
                                                             :created-by-uri created-by-uri
                                                             :active false})))))
