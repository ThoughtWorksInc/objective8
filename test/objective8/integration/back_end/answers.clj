(ns objective8.integration.back-end.answers
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.config :as config]
            [objective8.core :as core]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.back-end.domain.objectives :as objectives]
            [objective8.back-end.domain.questions :as questions]
            [objective8.middleware :as m]
            [objective8.back-end.domain.answers :as answers]))

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

         (fact "returns a 404 status if a PSQLException is raised"
               (against-background
                 (answers/store-answer! anything) =throws=> (org.postgresql.util.PSQLException.
                                                              (org.postgresql.util.ServerErrorMessage. "" 0)))
               (get-in (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers")
                                  :request-method :post
                                  :content-type "application/json"
                                  :body (json/generate-string the-answer))
                       [:response :status]) => 404)

         (fact "returns a 404 status if a map->answer exception is raised"
               (get-in (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers")
                                  :request-method :post
                                  :content-type "application/json"
                                  :body (json/generate-string the-invalid-answer))
                       [:response :status]) => 404)

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

         (fact "returns a 404 status if the question does not belong to the objective"
               (let [{q-id :_id o-id :objective-id} (sh/store-a-question)
                     {response :response} (p/request app (str "/api/v1/objectives/" (inc o-id) 
                                                              "/questions/" q-id "/answers")
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body "")]
                 (:status response) => 404))))

(facts "GET /api/v1/objectives/:id/questions/:id/answers"
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "retrieves answers for a question"
               (let [{objective-id :objective-id q-id :_id :as question} (sh/store-a-question)
                     stored-answers (doall (->> (repeat {:question question})
                                                (take 5)
                                                (map sh/store-an-answer)))
                     answer-uri (str "/objectives/" objective-id "/questions/" q-id "/answers/")
                     {response :response} (p/request app (str "/api/v1/objectives/" objective-id "/questions/" q-id "/answers"))]
                 (:body response) => (helpers/json-contains (map contains (->> stored-answers
                                                                               reverse
                                                                               (map #(dissoc % :global-id))
                                                                               (map #(assoc % :uri (contains answer-uri))))))))

         (fact "retrieves vote count for each answer"
               (let [{objective-id :objective-id q-id :_id :as question} (sh/store-a-question)
                     {global-id :global-id} (sh/store-an-answer {:question question})
                     up-votes (doall (for [_ (range 5)] (sh/store-an-up-down-vote global-id :up)))
                     down-votes (doall (for [_ (range 3)] (sh/store-an-up-down-vote global-id :down)))
                     {response :response} (p/request app (utils/path-for :api/get-answers-for-question
                                                                         :id objective-id
                                                                         :q-id q-id))]
                 (:body response) => (helpers/json-contains [(contains {:votes {:up 5 :down 3}})])))

         (fact "returns a 404 status if the question does not belong to the objective"
               (let [{q-id :_id o-id :objective-id} (sh/store-a-question)
                     {response :response} (p/request app (str "/api/v1/objectives/" (inc o-id)
                                                              "/questions/" q-id "/answers"))]
                 (:status response) => 404))))

(facts "GET /api/v1/objectives/:id/questions/:id/answers?sorted-by=up-votes"
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "retrieves answers ordered by up-votes"
               (let [{objective-id :objective-id question-id :_id :as question} (sh/store-a-question)
                     {least-up-votes-id :_id least-up-votes-g-id :global-id} (sh/store-an-answer {:question question})
                     {most-up-votes-id :_id most-up-votes-g-id :global-id} (sh/store-an-answer {:question question})
                     question-uri (str "/objectives/" objective-id "/questions/" question-id)] 

                 (sh/store-an-up-down-vote most-up-votes-g-id :up) 
                 (sh/store-an-up-down-vote most-up-votes-g-id :up) 
                 (sh/store-an-up-down-vote least-up-votes-g-id :up) 

                 (-> (p/request app (str (utils/path-for :api/get-answers-for-question
                                                         :id objective-id
                                                         :q-id question-id) "?sorted-by=up-votes"))
                     (get-in [:response :body])) => (helpers/json-contains [(contains {:_id most-up-votes-id})
                                                                            (contains {:_id least-up-votes-id})])))))

(facts "GET /api/v1/objectives/:id/questions/:id/answers?sorted-by=down-votes"
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "retrieves answers ordered by down-votes"
               (let [{objective-id :objective-id question-id :_id :as question} (sh/store-a-question)
                     {least-down-votes-id :_id least-down-votes-g-id :global-id} (sh/store-an-answer {:question question})
                     {most-down-votes-id :_id most-down-votes-g-id :global-id} (sh/store-an-answer {:question question})
                     question-uri (str "/objectives/" objective-id "/questions/" question-id)] 

                 (sh/store-an-up-down-vote most-down-votes-g-id :down) 
                 (sh/store-an-up-down-vote most-down-votes-g-id :down) 
                 (sh/store-an-up-down-vote least-down-votes-g-id :down) 

                 (-> (p/request app (str (utils/path-for :api/get-answers-for-question
                                                         :id objective-id
                                                         :q-id question-id) "?sorted-by=down-votes"))
                     (get-in [:response :body])) => (helpers/json-contains [(contains {:_id most-down-votes-id})
                                                                            (contains {:_id least-down-votes-id})])))))

(facts "GET /api/v1/objectives/:id/questions/:id/answers?filter-type=has-writer-note"
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "retrieves answers that have writer notes"
              (let [{objective-id :objective-id question-id :_id :as question} (sh/store-a-question)
                    answer-without-note (sh/store-an-answer {:question question :answer-text "without note"})
                    answer-with-note (-> (sh/store-an-answer {:question question :answer-text "with note"})
                                         (sh/with-note "writer note content"))
                    {response :response} (p/request app (str (utils/path-for :api/get-answers-for-question
                                                                             :id objective-id
                                                                             :q-id question-id) "?filter-type=has-writer-note"))]
                (:body response) => (helpers/json-contains [(contains {:_id (:_id answer-with-note)
                                                                       :answer "with note"
                                                                       :note "writer note content"})])
                (:body response) =not=> (helpers/json-contains [(contains {:_id (:_id answer-without-note)})])))))

(facts "GET /api/v1/objectives/:id/questions/:id/answers?limit=<n>"
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "retrieves answers offset as indicated"
               (let [{o-id :objective-id q-id :_id :as question} (sh/store-a-question)
                     stored-answers (doall (->> (repeat {:question question})
                                                (take 10)
                                                (map sh/store-an-answer)))
                     p-response (p/request app (str (utils/path-for :api/get-answers-for-question
                                                                    :id o-id
                                                                    :q-id q-id)
                                                    "?offset=7"))]

                 (count (helpers/peridot-response-json-body->map p-response)) => 3))))
