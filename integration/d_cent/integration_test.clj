(ns d-cent.integration-test
  (:require [ring.mock.request :as mock]
            [midje.sweet :refer :all]
            [d-cent.core :as core]
            [d-cent.objectives :refer [request->objective]]
            [d-cent.storage :as storage]
            [d-cent.user :as user]))

(def user-id "twitter-user_id")
(def email-address "test@email.address.com")

(defn with-signed-in-user [request]
  (into request {:session
                 {:cemerick.friend/identity
                  {:authentications
                   {user-id {:identity user-id
                             :username "screen name"
                             :roles #{:signed-in}}}
                   :current user-id}}}))

(def objectives-create-request (mock/request :get "/objectives/create"))
(def objectives-post-request (mock/request :post "/objectives") )
(def email-capture-get-request (mock/request :get "/email"))
(def email-capture-post-request (mock/request :post "/email"))

(def app (core/app core/app-config))

(facts "authorisation"
       (future-facts "signed in users"
              (fact "can reach the create objective page"
                    (app (-> objectives-create-request with-signed-in-user))
                    => (contains {:status 200}))
              (fact "can post a new objective"
                (let [request (-> objectives-post-request with-signed-in-user)]
                    (app request)
                    => (contains {:status 201})
                    (provided
                      (request->objective anything) => :an-objective
                      (storage/store! anything :an-objective) => :stored-objective )))
              (fact "can reach the email capture page"
                    (app (-> email-capture-get-request with-signed-in-user))
                    => (contains {:status 200}))
              (fact "can post their email address"
                    (app (-> email-capture-post-request with-signed-in-user))
                    => (contains {:status 200})))

       (facts "unauthorised users"
              (fact "cannot reach the objective creation page"
                    (app objectives-create-request)
                    => (contains {:status 302}))
              (fact "cannot post a new objective"
                    (app objectives-post-request)
                    => (contains {:status 401}))
              (fact "cannot reach the email capture page"
                    (app email-capture-get-request)
                    => (contains {:status 302}))
              (fact "cannot post their email address"
                    (app email-capture-post-request)
                    => (contains {:status 401}))))

(future-fact "should be able to store email addresses"
      (do
        (app (-> email-capture-post-request with-signed-in-user))
        
        (user/find-email-address-for-user {} user-id))
      => email-address)
