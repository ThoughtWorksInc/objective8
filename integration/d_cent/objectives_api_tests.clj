(ns d-cent.objectives-api-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [d-cent.storage :as storage]
            [d-cent.utils :as utils]
            [d-cent.core :as core]
            [d-cent.integration-helpers :as helpers]))

(def test-db (atom {}))
(def app (helpers/test-context test-db))

(def the-objective {:title "my objective title"
                    :goals "my objective goals"
                    :description "my objective description"
                    :end-date "2012-12-2"
                    :created-by "some dude"})

(def date-time (utils/string->date-time (the-objective :end-date)))
(def string-time (str date-time))

(fact "can get an objective using an id"
      (storage/store! test-db "objectives" {:_id "12345" :is "an objective" :end-date (utils/string->date-time "2012-12-12")})
      (let [objective-id (:_id (first (get @test-db "objectives")))
            objective-request (p/request app (str "/api/v1/objectives/" objective-id))]
        (-> objective-request :response :status) => 200
        (-> objective-request :response :body) => (contains "\"is\":\"an objective\"")))

(fact "returns a 404 if an objective does not exist"
      (let [objective-id "no-existy"
            objective-request (p/request app (str "/api/v1/objectives/" objective-id))]
        (-> objective-request :response :status) => 404))

(fact "Objectives posted to the API get stored"
      (let [request-to-create-objective (p/request app "/api/v1/objectives"
                                                   :request-method :post
                                                   :content-type "application/json"
                                                   :body (json/generate-string (assoc the-objective :end-date string-time)))
            response (:response request-to-create-objective)
            headers (:headers response)]
        response => (contains {:status 201})
        headers => (contains {"Location" (contains "/api/v1/objectives/")})
        (storage/find-by test-db "objectives" (constantly true)) => (contains (assoc the-objective :end-date date-time))))
