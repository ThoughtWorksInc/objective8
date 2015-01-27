(ns d-cent.integration-test
  (:require [ring.mock.request :as mock]
            [midje.sweet :refer :all]
            [d-cent.core :as core]))

(defn with-signed-in-user [request]
  (into request {:session
                 {:cemerick.friend/identity
                  {:authentications
                   {"screen name" {:identity "screen name"
                                   :username "screen name"
                                   :roles #{:signed-in}}}
                   :current "screen name"}}}))

(def objectives-create-request (mock/request :get "/objectives/create"))
(def email-capture-get-request (mock/request :get "/email"))
(def email-capture-post-request (mock/request :post "/email"))

(facts "authorization"
       (facts "signed in users"
              (fact "can reach the create objective page"
                    (core/app (-> objectives-create-request with-signed-in-user))
                    => (contains {:status 200}))
              (fact "can reach the email capture page"
                    (core/app (-> email-capture-get-request with-signed-in-user))
                    => (contains {:status 200}))
              (fact "can post their email address"
                    (core/app (-> email-capture-post-request with-signed-in-user))
                    => (contains {:status 200})))

       (facts "unauthorised users"
              (fact "cannot reach the objective creation page"
                    (core/app objectives-create-request)
                    => (contains {:status 302}))
              (fact "cannot reach the email capture page"
                    (core/app email-capture-get-request)
                    => (contains {:status 302}))
              (fact "cannot post their email address"
                    (core/app email-capture-post-request)
                    => (contains {:status 401}))))

