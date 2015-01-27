(ns d-cent.integration-test
  (:require [ring.mock.request :as mock]
            [midje.sweet :refer :all]
            [d-cent.core :as core]))

(def objectives-create-request (mock/request :get "/objectives/create"))
(def email-capture-request (mock/request :get "/email"))

(defn with-signed-in-user [request]
  (into request {:session
                 {:cemerick.friend/identity
                  {:authentications
                   {"screen name" {:identity "screen name"
                                   :username "screen name"
                                   :roles #{:signed-in}}}
                   :current "screen name"}}}))

(facts "About authorization"
       (fact "A signed in user can reach the create objective page"
             (core/app (-> objectives-create-request with-signed-in-user))
             => (contains {:status 200}))
       (fact "A signed in user can reach the email capture page"
             (core/app (-> email-capture-request with-signed-in-user))
             => (contains {:status 200}))


       (fact "An unauthorised user cannot reach the objective creation page"
             (core/app objectives-create-request)
             => (contains {:status 302}))
       (fact "An unauthorised user cannot reach the email capture page"
             (core/app email-capture-request)
             => (contains {:status 302})))
