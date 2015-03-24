(ns objective8.integration.api.objectives
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.objectives :as objectives]
            [objective8.users :as users]
            [objective8.middleware :as m]))

(def app (helpers/test-context))

(def OBJECTIVE_ID 10)

(def the-objective {:title "my objective title"
                    :goal-1 "my first objective goal"
                    :goal-2 "my second objective goal"
                    :description "my objective description"
                    :end-date "2015-01-01"
                    :created-by-id 1})

(def the-invalid-objective {:title "my objective title"
                            :goal-1 "my first objective goal"
                            :goal-2 "my second objective goal"
                            :description "my objective description"
                            :end-date "2015-01-01"})

(def stored-objective (assoc the-objective :_id OBJECTIVE_ID))

(defn gen-user-with-id
  "Make a user and return the ID for use in creating other content"
  []
  (:_id (users/store-user! {:twitter-id "anything" :username "username"})))

(facts "objectives"
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (facts "GET /api/v1/objectives returns a list of objectives in reverse chronological order"
               (fact "objectives are returned as a list"
                     (let [stored-user-id (gen-user-with-id)
                           objective-1 (objectives/store-objective! (assoc the-objective :created-by-id stored-user-id))
                           objective-2 (objectives/store-objective! (assoc the-objective :created-by-id stored-user-id))
                           objective-3 (objectives/store-objective! (assoc the-objective :created-by-id stored-user-id))
                           parsed-response (helpers/peridot-response-json-body->map (p/request app "/api/v1/objectives"))]
                       parsed-response => list?
                       parsed-response => [(assoc objective-3 :username "username")
                                           (assoc objective-2 :username "username")
                                           (assoc objective-1 :username "username")]))

               (fact "returns an empty list if there are no objectives"
                     (do
                       (helpers/truncate-tables)
                       (helpers/peridot-response-json-body->map (p/request app "/api/v1/objectives")))
                     => empty?))

        (facts "GET /api/v1/objectives/:id returns an objective"
               (fact "can retrieve an objective using its id"
                     (let [{user-id :_id username :username} (sh/store-a-user) 
                           stored-objective (objectives/store-objective! (assoc the-objective :created-by-id user-id))
                           objective-url (str "/api/v1/objectives/" (:_id stored-objective))]
                       (get-in (p/request app objective-url)
                               [:response :body]) => (helpers/json-contains (assoc stored-objective :username username))))

               (fact "returns a 404 if an objective does not exist"
                     (p/request app (str "/api/v1/objectives/" 123456))
                     => (contains {:response (contains {:status 404})})) 

               (fact "returns an error if objective id is not an integer"
                     (p/request app "/api/v1/objectives/NOT-AN-INTEGER")
                     => (contains {:response (contains {:status 404})}))) 

        (facts "about posting objectives"
               (against-background
                (m/valid-credentials? anything anything anything) => true)

               (fact "the posted objective is stored"
                     (let [{user-id :_id} (sh/store-a-user)
                           the-objective {:title "my objective title"
                                          :goal-1 "my first objective goal"
                                          :end-date "2015-01-01"
                                          :created-by-id user-id}
                           {response :response} (p/request app "/api/v1/objectives"
                                                           :request-method :post
                                                           :content-type "application/json"
                                                           :body (json/generate-string the-objective))]
                       (:body response) => (helpers/json-contains (assoc the-objective :uri (contains "/objectives/")))
;                       (:body response) =not=> (helpers/json-contains {:global-id anything})
                       (:headers response) => (helpers/location-contains (str "/api/v1/objectives/"))
                       (:status response) => 201))

               (fact "a 400 status is returned if a PSQLException is raised"
                     (against-background
                      (objectives/store-objective! anything) =throws=> (org.postgresql.util.PSQLException.
                                                                        (org.postgresql.util.ServerErrorMessage. "" 0)))
                     (:response (p/request app "/api/v1/objectives"
                                           :request-method :post
                                           :content-type "application/json"
                                           :body (json/generate-string the-objective))) => (contains {:status 400}))

               (fact "a 400 status is returned if a map->objective exception is raised"
                     (:response (p/request app "/api/v1/objectives"
                                           :request-method :post
                                           :content-type "application/json"
                                           :body (json/generate-string the-invalid-objective))) => (contains {:status 400})))))
