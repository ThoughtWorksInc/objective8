(ns d-cent.api_test
  (:require [midje.sweet :refer :all]
            [cheshire.core :as json]
            [d-cent.core :as core]
            [peridot.core :as p]
            [d-cent.utils :as utils]
            [d-cent.storage :as s]))

(def temp-store (atom {}))
(def app-session (p/session (core/app (assoc core/app-config :store temp-store))))

(def the-objective {:title "my objective title"
                    :goals "my objective goals"
                    :description "my objective description"
                    :end-date "my objective end-date"
                    :created-by "some dude"})

(fact "Objectives posted to the API get stored"
      (let [request-to-create-objective (p/request app-session "/api/v1/objectives"
                                                   :request-method :post
                                                   :content-type "application/json"
                                                   :body (json/generate-string the-objective))
            response (:response request-to-create-objective)
            headers (:headers response)]
        response => (contains {:status 201})
        headers => (contains {"Location" "value"})
        (s/find-by temp-store "objectives" (constantly true)) => (contains the-objective)))

