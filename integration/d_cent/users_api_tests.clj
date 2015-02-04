(ns d-cent.users-api-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [cheshire.core :as json]
            [d-cent.core :as core]
            [d-cent.storage :as s]
            [d-cent.user :as user]
            [d-cent.integration-helpers :as helpers]))

;; Testing from http request -> making correct calls within user namespace
;; Mock or stub out 'user' namespace

(def the-user-id "twitter-user_id")
(def the-email-address "test@email.address.com")

(def test-db (atom {}))
(def app (helpers/test-context test-db))

(def user {:_id "SOME_GUID"
           :user-id "someTwitterID"
           :email-address "something@something.com"})

(fact "A user profile can be retrieved"
      (let [request-to-get-user-profile (p/request app "/api/v1/users?twitter=someTwitterID")
            response (:response request-to-get-user-profile)]
        (response :body)) => (json/generate-string user)
        (provided (user/retrieve-user-record anything "someTwitterID") => user))

(fact "returns a 404 if a user does not exist"
      (let [twitter-id "no-existy"
            user-request (p/request app (str "/api/v1/users?twitter=" twitter-id))]
        (-> user-request :response :status) => 404))

(fact "The API can be used to store a user profile"
       (let [app-config (into core/app-config {:store test-db})
             user-session (p/session (core/app app-config))
             api-response (-> user-session
                              (p/content-type "application/json")
                              (p/request "/api/v1/users"
                                         :request-method :post
                                         :headers {"Content-Type" "application/json"}
                                         :body (json/generate-string {:email-address the-email-address
                                                                      :user-id the-user-id}))
                              :response)
             stored-email (:email-address (user/retrieve-user-record test-db the-user-id))]

         stored-email => the-email-address
         api-response => (contains {:status 201})
         api-response => (contains {:body string?})
         (json/parse-string (:body api-response) true) => (contains {:_id anything})
         api-response => (contains {:headers (contains {"Location" (contains "/api/v1/users/")})})))
