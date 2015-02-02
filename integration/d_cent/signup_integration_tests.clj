(ns d-cent.signup-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [cemerick.friend.workflows :as workflows]
            [d-cent.core :as core]
            [d-cent.integration-helpers :as helpers]
            [d-cent.http-api :as api]))

(def test-session (helpers/test-context))

(def twitter-callback-url "http://localhost:8080/twitter-callback?oauth_verifier=VERIFICATION_TOKEN")
(def sign-up-url "http://localhost:8080/sign-up")

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
      (let [response (p/request test-session twitter-callback-url)
            ;TODO: follow redirect and assert that it contains expected content
            ]
        response => (check-redirects-to "/sign-up")))

(fact "New users signing in via twitter can create user profiles"
      (against-background
       (oauth/access-token anything anything anything) => {:user_id "USERID"}
       (api/create-user-profile {:user-id "twitter-USERID"
                                 :email-address "test@email.address.com"})
       => {:_id "SOME_GUID" :user-id "twitter-USERID" :email-address "test@email.address.com"})
      (let [signed-in-session (p/request test-session twitter-callback-url)
            response (p/request signed-in-session sign-up-url
                                :request-method :post
                                :content-type "application/x-www-form-urlencoded"
                                :body "email-address=test%40email.address.com")
            ;TODO: attempt to access protected URI; assert that we get there
            ]
        response => (check-redirects-to "/" 303)))

(fact "Returning users with profiles are authenticated without being requested for email address"
      (against-background
       (oauth/access-token anything anything anything) => {:user_id "USERID"}
       (api/find-user-profile-by-user-id "twitter-USERID") => {:_id "SOME_GUID"
                                                              :user-id "twitter-USERID"
                                                              :email-address "test@email.address.com"})
      (let [signed-in-session (p/request test-session twitter-callback-url)
            response (p/follow-redirect signed-in-session)
            ;TODO: attempt to access protected URI; assert that we get there
            ]
        response => (check-redirects-to "/" 303)))
