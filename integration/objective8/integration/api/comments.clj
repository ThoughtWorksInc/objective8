(ns objective8.integration.api.comments
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration-helpers :as helpers]
            [objective8.storage-helpers :as sh]
            [objective8.middleware :as m]
            [objective8.comments :as comments]))

;; Testing from http request -> making correct calls within comments namespace
;; Mock or stub out 'comments' namespace

(def app (helpers/test-context))

(def OBJECTIVE_ID 234)
(def USER_ID 1)

(def the-comment {:comment "The comment"
                  :objective-id OBJECTIVE_ID
                  :created-by-id USER_ID})

(def the-comment-as-json (json/generate-string the-comment))
(def the-invalid-comment {:comment "The comment"
                          :objective-id OBJECTIVE_ID })

(defn a-comment [objective-id created-by-id]
  {:comment "The comment"
   :objective-id objective-id
   :created-by-id created-by-id})

(facts "POST /api/v1/objectives/:id/comments" :integration
       (against-background
        (m/valid-credentials? anything anything anything) => true)
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "the posted comment is stored"
              (let [{obj-id :_id user-id :created-by-id} (sh/store-an-objective)
                    comment (a-comment obj-id user-id)
                    {response :response} (p/request app "/api/v1/comments"
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string comment))]
                (:body response) => (helpers/json-contains (assoc comment :_id integer?))
                (:status response) => 201
                (:headers response) => (helpers/location-contains (str "/api/v1/comments/"))))

        (fact "a 400 status is returned if a PSQLException is raised"
              (against-background
               (comments/store-comment! anything) =throws=> (org.postgresql.util.PSQLException. 
                                                             (org.postgresql.util.ServerErrorMessage. "" 0)))
              (:response (p/request app "/api/v1/comments"
                                    :request-method :post
                                    :content-type "application/json"
                                    :body the-comment-as-json)) => (contains {:status 400}))

        (fact "a 400 status is returned if a map->comment exception is raised"
              (:response (p/request app "/api/v1/comments"
                                    :request-method :post
                                    :content-type "application/json"
                                    :body (json/generate-string the-invalid-comment))) => (contains {:status 400}))

        (fact "a 423 (resource locked) status is returned when drafting has started on the objective"
              (let [{obj-id :_id user-id :created-by-id} (sh/store-an-objective-in-draft)
                    comment (a-comment obj-id user-id)
                    {response :response} (p/request app (str "/api/v1/comments")
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string comment))]
                (:status response) => 423))))


(facts "GET /api/v1/objectives/:id/comments" :integration
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "retrieves comments for an objective ID"
              (let [objective (sh/store-an-objective)
                    stored-comments (doall (->> (repeat {:objective objective})
                                                (take 5)
                                                (map sh/store-a-comment)
                                                (map #(dissoc % :username))))
                    {response :response} (p/request app (str "/api/v1/objectives/" (:_id objective) "/comments"))]
                 (:body response) => (helpers/json-contains (map contains stored-comments))))))
