(ns objective8.api.objectives-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration-helpers :as helpers]
            [objective8.objectives :as objectives]
            [cheshire.core :as json]))

;; Testing from http request -> making correct calls within objectives namespace
;; Mock or stub out 'objectives' namespace

(def test-db (atom {}))
(def app (helpers/test-context test-db))

(def OBJECTIVE_ID 10)

(def the-objective {:title "my objective title"
                    :goals "my objective goals"
                    :description "my objective description"
                    :end-date "2015-01-01"
                    :created-by-id 1})

(def stored-objective (assoc the-objective :_id OBJECTIVE_ID))

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
               headers => (contains {"Location" (contains (str "/api/v1/objectives/" OBJECTIVE_ID))}))))
