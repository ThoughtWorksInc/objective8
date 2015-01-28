(ns d-cent.integration-test
  (:require [ring.mock.request :as mock]
            [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [d-cent.core :as core]
            [d-cent.objectives :refer [request->objective]]
            [d-cent.storage :as storage]
            [d-cent.user :as user]))

(def user-id "twitter-user_id")
(def email-address "test@email.address.com")

(def objectives-create-request (mock/request :get "/objectives/create"))
(def objectives-post-request (mock/request :post "/objectives"))
(def email-capture-get-request (mock/request :get "/email"))
(def email-capture-post-request (mock/request :post "/email"))

(def app (core/app core/app-config))

(defn access-as-signed-in-user 
  "Requires oauth/request-token and oauth/access-token to be stubbed in background or provided statements"
  [url & args]
  (let [twitter-sign-in (-> (p/session app)
                            (p/request "/twitter-sign-in" :request-method :post)
                            (p/request "/twitter-callback?oauth_verifier=the-verifier"))]
    (apply p/request twitter-sign-in url args)))

(defn check-status [status]
  (fn [peridot-response]
    (= status (get-in peridot-response [:response :status]))))

(facts "authorisation"
       (facts "signed in users"
              (background (oauth/request-token anything anything) => {:oauth_token "the-token"}
                          (oauth/access-token anything anything anything) => {:user_id "the-user-id"})
              (fact "can reach the create objective page"
                    (access-as-signed-in-user "/objectives/create")
                    => (check-status 200))
              (fact "can post a new objective"
                    (access-as-signed-in-user "/objectives" :request-method :post)
                    => (check-status 201)
                    (provided
                     (request->objective anything) => :an-objective
                     (storage/store! anything :an-objective) => :stored-objective))
              (fact "can reach the email capture page"
                    (access-as-signed-in-user "/email")
                    => (check-status 200))
              (fact "can post their email"
                    (access-as-signed-in-user "/email" :request-method :post)
                    => (check-status 200)))

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
