(ns d-cent.api-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [cheshire.core :as json]
            [d-cent.core :as core]
            [d-cent.utils :as utils]
            [d-cent.storage :as s]
            [d-cent.user :as user]))

(def the-user-id "twitter-user_id")
(def the-email-address "test@email.address.com")

(def temp-store (atom {}))
(def app-session (p/session (core/app (assoc core/app-config :store temp-store))))

(def the-objective {:title "my objective title"
                    :goals "my objective goals"
                    :description "my objective description"
                    :end-date "2012-12-2"
                    :created-by "some dude"})
(def user {:_id "SOME_GUID"
           :user-id "someTwitterID"
           :email-address "something@something.com"})


(fact "Objectives posted to the API get stored"
      (let [request-to-create-objective (p/request app-session "/api/v1/objectives"
                                                   :request-method :post
                                                   :content-type "application/json"
                                                   :body (json/generate-string (update-in the-objective [:end-date] #(str (utils/string->time-stamp %)))))
            response (:response request-to-create-objective)
            headers (:headers response)]
        response => (contains {:status 201})
        headers => (contains {"Location" (contains "/api/v1/objectives/")})
        (s/find-by temp-store "objectives" (constantly true)) => (contains (update-in the-objective [:end-date] utils/string->time-stamp))))

(fact "A user profile can be retrieved"
      (let [request-to-get-user-profile (p/request app-session "/api/v1/users?twitter=someTwitterID")
            response (:response request-to-get-user-profile)]
        (response :body)) => (json/generate-string user)
        (provided (user/retrieve-user-record anything "someTwitterID") => user))

(fact "The API can be used to store a user profile"
       (let [temp-store (atom {})
             app-config (into core/app-config {:store temp-store})
             user-session (p/session (core/app app-config))
             api-response (-> user-session
                              (p/content-type "application/json")
                              (p/request "/api/v1/users"
                                         :request-method :post
                                         :headers {"Content-Type" "application/json"}
                                         :body (json/generate-string {:email-address the-email-address
                                                                      :user-id the-user-id}))
                              :response)
             stored-email (:email-address (user/retrieve-user-record temp-store the-user-id))]

         stored-email => the-email-address
         api-response => (contains {:status 201})
         api-response => (contains {:body string?})
         (json/parse-string (:body api-response) true) => (contains {:_id anything})
         api-response => (contains {:headers (contains {"Location" (contains "/api/v1/users/")})})))
