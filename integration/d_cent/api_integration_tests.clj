(ns d-cent.api-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [cheshire.core :as json]
            [d-cent.core :as core]
            [d-cent.storage :as storage]
            [d-cent.user :as user]))

(def the-user-id "twitter-user_id")
(def the-email-address "test@email.address.com")

(fact "should be able to store email addresses"
      (let [store (atom {})
            app-config (into core/app-config {:store store})
            app (core/app app-config)]
        (do
          (-> (p/session app)
              (p/content-type "application/json")
              (p/request "/api/v1/users"
                         :request-method :post
                         :headers {"Content-Type" "application/json"}
                         :body (json/generate-string {:email-address the-email-address
                                                      :user-id the-user-id})))
          (:email-address (user/retrieve-user-record store the-user-id))))
      => the-email-address)

