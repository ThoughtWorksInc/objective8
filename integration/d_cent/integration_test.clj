(ns d-cent.integration-test
  (:require [ring.mock.request :as mock]
            [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [cheshire.core :as json]
            [d-cent.core :as core]
            [d-cent.objectives :refer [request->objective]]
            [d-cent.storage :as storage]
            [d-cent.user :as user]))

(def the-user-id "twitter-user_id")
(def the-email-address "test@email.address.com")

(def objectives-create-request (mock/request :get "/objectives/create"))
(def objectives-post-request (mock/request :post "/objectives"))
(def objective-view-get-request (mock/request :get "/objectives/some-long-id"))
(def email-capture-get-request (mock/request :get "/email"))
(def user-profile-post-request (mock/request :post "/users"))

(def default-app (core/app core/app-config))

(def twitter-authentication-background
  (background (oauth/request-token anything anything) => {:oauth_token "the-token"}
              (oauth/access-token anything anything anything) => {:user_id the-user-id}))

(defn access-as-signed-in-user
  "Requires oauth/request-token and oauth/access-token to be stubbed in background or provided statements"
  [app url & args]
  (let [twitter-sign-in (-> (p/session app)
                            (p/request "/twitter-sign-in" :request-method :post)
                            (p/request "/twitter-callback?oauth_verifier=the-verifier"))]
    (apply p/request twitter-sign-in url args)))

(defn check-status [status]
  (fn [peridot-response]
    (= status (get-in peridot-response [:response :status]))))

(facts "authorisation"
       (facts "signed in users"
              twitter-authentication-background
              (fact "can reach the create objective page"
                    (access-as-signed-in-user default-app "/objectives/create")
                    => (check-status 200))
              (fact "can post a new objective"
                    (access-as-signed-in-user default-app "/objectives" :request-method :post)
                    => (check-status 201)
                    (provided
                     (request->objective anything) => :an-objective
                     (storage/store! anything anything :an-objective) => :stored-objective))
              (fact "can reach the email capture page"
                    (access-as-signed-in-user default-app "/email")
                    => (check-status 200))
              (future-fact "can post their email"
                    (access-as-signed-in-user default-app "/api/v1/users" :request-method :post)
                    => (check-status 200)))

       (facts "unauthorised users"
              (fact "cannot reach the objective creation page"
                    (default-app objectives-create-request)
                    => (contains {:status 302}))
              (fact "cannot post a new objective"
                    (default-app objectives-post-request)
                    => (contains {:status 401}))
              (fact "cannot reach the email capture page"
                    (default-app email-capture-get-request)
                    => (contains {:status 302}))
              (fact "cannot post their user profile"
                    (default-app user-profile-post-request)
                    => (contains {:status 302}))
              (fact "can access objective view"
                    (default-app objective-view-get-request)
                    => (contains {:status 200})
                    (provided
                      (storage/find-by anything anything "some-long-id") => :an-objective))))

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

(fact "authorised user can post and retrieve objective"
      twitter-authentication-background
       (let [store (atom {})
            app-config (into core/app-config {:store store})
            app (core/app app-config)
             params {:title "my objective title"
                     :goals "my objective goals"
                     :description "my objective description"
                     :end-date "my objective end-date"}]
        (do
          (access-as-signed-in-user app "/objectives" :request-method :post
                                    :params params)
          (storage/find-by store "objectives" #(= (:username %) the-user-id)))
             => (contains params)))
