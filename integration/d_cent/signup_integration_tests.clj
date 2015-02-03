(ns d-cent.signup-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [cemerick.friend.workflows :as workflows]
            [d-cent.core :as core]
            [d-cent.integration-helpers :as helpers]
            [d-cent.http-api :as api]))

(def test-store (atom {}))
(def test-session (helpers/test-context test-store))

(def twitter-callback-url "http://localhost:8080/twitter-callback?oauth_verifier=VERIFICATION_TOKEN")
(def sign-up-url "http://localhost:8080/sign-up")
(def protected-resource "http://localhost:8080/objectives/create")

(defn check-redirects-to 
  ([url-fragment]
   (check-redirects-to url-fragment 302))
  ([url-fragment status]
   (contains {:response 
              (contains {:status status
                         :headers (contains {"Location" (contains url-fragment)})})})))

(fact "Users signing in with twitter are redirected to sign-up page"
      (against-background
       (oauth/access-token anything anything anything) => {:user_id "USERID"})

      (p/request test-session twitter-callback-url) => (check-redirects-to "/sign-up"))

(fact "After signing up (by posting their email address) the user is sent back to the resource they were trying to access"
      (against-background
       (oauth/access-token anything anything anything) => {:user_id "USERID"})

      (let [unauthorized-request-session (p/request test-session protected-resource)
            signed-in-session (p/request unauthorized-request-session twitter-callback-url)]
        (p/request signed-in-session sign-up-url
                   :request-method :post
                   :content-type "application/x-www-form-urlencoded"
                   :body "email-address=test%40email.address.com"))
      => (check-redirects-to protected-resource 303)
      
      (provided (api/create-user-profile {:user-id "twitter-USERID"
                                          :email-address "test@email.address.com"})
                => {:_id "SOME_GUID" 
                    :user-id "twitter-USERID"
                    :email-address "test@email.address.com"} :times 1))

(fact "After signing in, a user with an existing profile is immediately sent to the resource they were trying to access"
      (against-background
       (oauth/access-token anything anything anything) => {:user_id "USERID"})

      (let [unauthorized-request-session (p/request test-session protected-resource)
            signed-in-session (p/request unauthorized-request-session twitter-callback-url)]
        (p/follow-redirect signed-in-session)) => (check-redirects-to protected-resource 303)

        (provided 
         (api/find-user-profile-by-user-id "twitter-USERID") 
         => {:_id "SOME_GUID"
             :user-id "twitter-USERID"
             :email-address "test@email.address.com"} :times 1))
