(ns d-cent.front-end.signup-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [cemerick.friend.workflows :as workflows]
            [ring.util.codec :as c]
            [d-cent.core :as core]
            [d-cent.config :as config]
            [d-cent.integration-helpers :as helpers]
            [d-cent.http-api :as api]
            [d-cent.utils :as utils]))

(def test-store (atom {}))
(def test-session (helpers/test-context test-store))

(def twitter-callback-url (str utils/host-url "/twitter-callback?oauth_verifier=VERIFICATION_TOKEN"))
(def sign-up-url (str utils/host-url "/sign-up"))
(def protected-resource (str utils/host-url "/objectives/create"))

(defn check-redirects-to
  ([url-fragment]
   (check-redirects-to url-fragment 302))
  ([url-fragment status]
   (contains {:response
              (contains {:status status
                         :headers (contains {"Location" (contains url-fragment)})})})))

(defn check-html-content [html-fragment]
  (contains {:response
             (contains {:body
                        (contains html-fragment)})}))

(defn check-status [status]
  (fn [{response :response}]
    ((contains {:status status}) response)))

(fact "User directly accessing /sign-up page is redirected to /sign-in"
      (p/request test-session sign-up-url) => (check-redirects-to "/sign-in"))

(fact "User directly posting to /sign-up page without csrf token receives 403"
      (p/request test-session sign-up-url
                 :request-method :post
                 :content-type "application/x-www-form-urlencoded"
                 :body "test%40email.address.com")
      => (check-status 403))

(fact "New users signing in with twitter are asked to sign up by entering their email address"
      (against-background
       (oauth/access-token anything anything anything) => {:user_id "USERID"}
       (api/find-user-profile-by-twitter-id "twitter-USERID") => nil)

      (let [signed-in-context (p/request test-session twitter-callback-url)]
        (p/follow-redirect signed-in-context))
      => (check-html-content "<title>Sign up"))

(binding [config/enable-csrf false]
(fact "After signing up (by posting their email address) the user is sent back to the resource they were trying to access"
      (against-background
       (oauth/access-token anything anything anything) => {:user_id "USERID"})

      (let [unauthorized-request-context (p/request test-session protected-resource)
            signed-in-context (p/request unauthorized-request-context twitter-callback-url)]
        (p/request signed-in-context sign-up-url
                   :request-method :post
                   :content-type "application/x-www-form-urlencoded"
                   :body "&email-address=test%40email.address.com"))
      => (check-redirects-to protected-resource 303)

      (provided (api/create-user-profile {:user-id "twitter-USERID"
                                          :email-address "test@email.address.com"})
                => {:_id "SOME_GUID"
                    :user-id "twitter-USERID"
                    :email-address "test@email.address.com"} :times 1)))

(fact "After signing in, a user with an existing profile is immediately sent to the resource they were trying to access"
      (against-background
       (oauth/access-token anything anything anything) => {:user_id "USERID"})

      (let [unauthorized-request-context (p/request test-session protected-resource)
            signed-in-context (p/request unauthorized-request-context twitter-callback-url)]
        (p/follow-redirect signed-in-context)) => (check-redirects-to protected-resource 303)

        (provided
         (api/find-user-profile-by-twitter-id "twitter-USERID")
         => {:_id "SOME_GUID"
             :user-id "twitter-USERID"
             :email-address "test@email.address.com"} :times 1))
