(ns d-cent.objectives-api-test
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [d-cent.storage :as storage]
            [d-cent.utils :as utils]
            [d-cent.integration-helpers :as helpers]))

(def test-db (atom {}))
(def app (helpers/test-context test-db))

(fact "can get an objective using an id"
      (storage/store! test-db "objectives" {:_id "12345" :is "an objective" :end-date (utils/string->time-stamp "2012-12-12")})
      (let [objective-id (:_id (first (get @test-db "objectives")))
            objective-request (p/request app (str "/api/v1/objectives/" objective-id))]
        (-> objective-request :response :status) => 200
        (-> objective-request :response :body) => (contains "\"is\":\"an objective\"")))

(fact "returns a 404 if an objective does not exist"
      (let [objective-id "no-existy"
            objective-request (p/request app (str "/api/v1/objectives/" objective-id))]
        (-> objective-request :response :status) => 404))
