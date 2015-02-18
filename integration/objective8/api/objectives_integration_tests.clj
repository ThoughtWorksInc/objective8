(ns objective8.api.objectives-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration-helpers :as helpers]
            [objective8.objectives :as objectives]
            [objective8.middleware :as m]
            ))

;; Testing from http request -> making correct calls within objectives namespace
;; Mock or stub out 'objectives' namespace


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

(facts "objectives" :integration
       (fact "can retrieve an objective using its id"
             (let [peridot-response (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID))]
               peridot-response) => (helpers/check-json-body stored-objective)
             (provided
               (objectives/retrieve-objective OBJECTIVE_ID) => stored-objective))

       (fact "returns a 404 if an objective does not exist"
             (against-background
               (objectives/retrieve-objective anything) => nil)

             (p/request app (str "/api/v1/objectives/" 123456))
             => (contains {:response (contains {:status 404})}))

       (fact "returns a 400 (Bad request) if objective id is not an integer"
             (p/request app "/api/v1/objectives/NOT-AN-INTEGER")
             => (contains {:response (contains {:status 400})}))

       (facts "about posting objectives"
              (against-background
                (m/valid-credentials? anything anything anything) => true)
              (fact "the posted objective is stored"
                    (let [peridot-response (p/request app "/api/v1/objectives"
                                                      :request-method :post
                                                      :content-type "application/json"
                                                      :body (json/generate-string the-objective))]
                      peridot-response)
                    => (helpers/check-json-body stored-objective)
                    (provided
                      (objectives/store-objective! the-objective) => stored-objective))

              (fact "the http response indicates the location of the objective"
                    (against-background
                      (objectives/store-objective! anything) => stored-objective)

                    (let [result (p/request app "/api/v1/objectives"
                                            :request-method :post
                                            :content-type "application/json"
                                            :body (json/generate-string the-objective))
                          response (:response result)
                          headers (:headers response)]
                      response => (contains {:status 201})
                      headers => (contains {"Location" (contains (str "/api/v1/objectives/" OBJECTIVE_ID))})))

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
                                          :body (json/generate-string the-invalid-objective))) => (contains {:status 400}))))
