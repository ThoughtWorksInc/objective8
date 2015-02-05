(ns d-cent.api.objectives-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [d-cent.utils :as utils]
            [d-cent.core :as core]
            [d-cent.integration-helpers :as helpers]
            [d-cent.objectives :as objectives]))

;; Testing from http request -> making correct calls within objectives namespace
;; Mock or stub out 'objectives' namespace

(def test-db (atom {}))
(def app (helpers/test-context test-db))

(def the-objective {:title "my objective title"
                    :goals "my objective goals"
                    :description "my objective description"
                    :end-date (utils/string->date-time "2015-01-01")
                    :created-by "USER_GUID"})

(def stored-objective (assoc the-objective :_id "OBJECTIVE_GUID"))

(def the-objective-as-json "{\"title\":\"my objective title\",\"goals\":\"my objective goals\",\"description\":\"my objective description\",\"end-date\":\"2015-01-01T00:00:00.000Z\",\"created-by\":\"USER_GUID\"}")

(def stored-objective-as-json "{\"_id\":\"OBJECTIVE_GUID\",\"title\":\"my objective title\",\"goals\":\"my objective goals\",\"description\":\"my objective description\",\"end-date\":\"2015-01-01T00:00:00.000Z\",\"created-by\":\"USER_GUID\"}")

(fact "can get an objective using its id"
      (p/request app (str "/api/v1/objectives/" "OBJECTIVE_GUID"))
      => (contains {:response
                    (contains {:body
                               (contains "\"end-date\":\"2015-01-01T00:00:00.000Z\"")})})
      (provided
       (objectives/retrieve-objective test-db "OBJECTIVE_GUID") => stored-objective))

(fact "returns a 404 if an objective does not exist"
      (against-background
       (objectives/retrieve-objective anything anything) => nil)
      
      (p/request app (str "/api/v1/objectives/" "ANY_GUID"))
      => (contains {:response (contains {:status 404})}))

(facts "about posting objectives"
       (fact "the posted objective is stored"
             (p/request app "/api/v1/objectives"
                        :request-method :post
                        :content-type "application/json"
                        :body the-objective-as-json)
             
             => (contains {:response (contains {:body (contains stored-objective-as-json)})})
             (provided
              (objectives/store-objective! test-db the-objective) => stored-objective))
       
       (fact "the http response indicates the location of the objective"

             (against-background
              (objectives/store-objective! anything anything) => stored-objective)
             
             (let [result (p/request app "/api/v1/objectives"
                                     :request-method :post
                                     :content-type "application/json"
                                     :body the-objective-as-json)
                   response (:response result)
                   headers (:headers response)]
               response => (contains {:status 201})
               headers => (contains {"Location" (contains "/api/v1/objectives/OBJECTIVE_GUID")}))))
