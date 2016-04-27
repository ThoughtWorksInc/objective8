(ns objective8.integration.front-end.signup
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.config :as config]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.front-end.api.http :as http-api]
            [objective8.utils :as utils]
            [objective8.front-end.workflows.sign-up :as sign-up]
            [stonecutter-oauth.client :as soc]
            [stonecutter-oauth.jwt :as so-jwt]
            [objective8.front-end.api.http :as http]
            [objective8.front-end.workflows.facebook :as facebook]
            [cheshire.core :as json]))

(def test-session (helpers/front-end-context))

(def twitter-callback-url (str utils/host-url "/twitter-callback?oauth_verifier=VERIFICATION_TOKEN"))
(def facebook-callback-url (str utils/host-url "/facebook-callback?code=1234455r6ftgyhu"))
(def stonecutter-callback-url (str utils/host-url "/d-cent-callback?code=1234567890ABCDEFGHIJ"))
(def sign-up-url (str utils/host-url "/sign-up"))
(def protected-resource (str utils/host-url "/objectives/create"))

(def USER_ID 1)

(defn check-html-content [html-fragment]
  (contains {:response
             (contains {:body
                        (contains html-fragment)})}))

(defn check-status [status]
  (fn [{response :response}]
    ((contains {:status status}) response)))

(facts "signup"
       (fact "User directly accessing /sign-up page is redirected to /sign-in"
             (p/request test-session sign-up-url) => (helpers/check-redirects-to "/sign-in"))

       (fact "User directly posting to /sign-up page without csrf token receives 403"
             (p/request test-session sign-up-url
                        :request-method :post
                        :content-type "application/x-www-form-urlencoded"
                        :body "test%40email.address.com")
             => (check-status 403))

       (fact "New users signing in with twitter are asked to sign up by entering their email address"
             (against-background
               (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
               (http-api/find-user-by-auth-provider-user-id "twitter-TWITTER_ID") => {:status ::http-api/not-found})
             (-> test-session
                 (p/request twitter-callback-url)
                 p/follow-redirect) => (check-html-content "<title>Sign up"))

       (fact "new users signing in with stonecutter are only asked to enter a username"
             (against-background
               (soc/request-access-token! anything "1234567890ABCDEFGHIJ") => {:id_token ...id-token...}
               (so-jwt/get-public-key-string-from-jwk-set-url anything) => ...public-key-string...
               (so-jwt/decode anything ...id-token... ...public-key-string...) => {:sub   "subject"
                                                                                   :email "email@test.com"}
               (http-api/find-user-by-auth-provider-user-id "d-cent-subject") => {:status ::http-api/not-found})
             (let [response (-> test-session
                                (p/request stonecutter-callback-url)
                                p/follow-redirect)]
               response => (every-checker (check-html-content "<title>Sign up")
                                          (check-html-content "clj-input-username"))
               response =not=> (check-html-content "clj-input-email-address")))

       (fact "new users signing in with facebook are only asked to enter a username"
             (against-background
               (facebook/get-access-token anything) => {:body (json/generate-string {:access_token "access-token-123"})}
               (facebook/get-token-info "access-token-123" anything) => {:body (json/generate-string {:data {:user_id "123"}})}
               (facebook/token-info-valid? {:user_id "123"} anything) => true
               (facebook/get-user-email "123") => {:body (json/generate-string {:email "valid@email.com"})}
               (http-api/find-user-by-auth-provider-user-id "facebook-123") => {:status ::http-api/not-found})
             (let [response (-> test-session
                                (p/request facebook-callback-url)
                                p/follow-redirect)]
               response => (every-checker (check-html-content "<title>Sign up")
                                          (check-html-content "clj-input-username"))
               response =not=> (check-html-content "clj-input-email-address")))

       (binding [config/enable-csrf false]
         (fact "After signing up (by posting their email address) the user is sent back to the resource they were trying to access"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id     "TWITTER_ID"
                                                                     :screen_name "SCREEN_NAME"}
                 (http-api/create-user {:auth-provider-user-id "twitter-TWITTER_ID"
                                        :username              "someUsername"
                                        :email-address         "test@email.address.com"})
                 => {:status ::http-api/success
                     :result {:_id                   USER_ID
                              :auth-provider-user-id "twitter-TWITTER_ID"
                              :username              "someUsername"
                              :email-address         "test@email.address.com"}})

               (let [unauthorized-request-context (p/request test-session protected-resource)
                     signed-in-context (p/request unauthorized-request-context twitter-callback-url)
                     sign-up-response (p/request signed-in-context sign-up-url
                                                 :request-method :post
                                                 :content-type "application/x-www-form-urlencoded"
                                                 :body "username=someUsername&email-address=test%40email.address.com")]
                 sign-up-response => (helpers/check-redirects-to protected-resource 303))

               (let [unauthorized-request-context (p/request test-session protected-resource)
                     signed-in-context (p/request unauthorized-request-context twitter-callback-url)
                     sign-up-response (p/request signed-in-context sign-up-url
                                                 :request-method :post
                                                 :content-type "application/x-www-form-urlencoded"
                                                 :body "username=someUsername&email-address=test%40email.address.com")]
                 sign-up-response) => anything
               (provided (sign-up/finalise-authorisation (contains {:_id USER_ID}) anything) => {})))

       (fact "After signing in, a user with an existing profile is immediately sent to the resource they were trying to access"
             (against-background
               (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"})

             (let [unauthorized-request-context (p/request test-session protected-resource)
                   signed-in-context (p/request unauthorized-request-context twitter-callback-url)]
               (p/follow-redirect signed-in-context)) => (helpers/check-redirects-to protected-resource 303)
             (provided
               (http-api/find-user-by-auth-provider-user-id "twitter-TWITTER_ID")
               => {:status ::http-api/success
                   :result {:_id                   USER_ID
                            :auth-provider-user-id "twitter-TWITTER_ID"
                            :email-address         "test@email.address.com"}} :times 1)))

(binding [config/enable-csrf false]
  (fact "unauthorised, unregistered user can sign in and be referred to a target uri"
        (against-background
          (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
          (http-api/create-user anything) => {:status ::http-api/success
                                              :result {:_id USER_ID}}
          (utils/safen-url "/target") => "/target")
        (let [target-uri "/target"
              user-session (helpers/front-end-context)
              sign-in-with-refer-response (-> user-session
                                              (p/request (str utils/host-url "/sign-in?refer=" target-uri))
                                              (p/request twitter-callback-url)
                                              (p/request sign-up-url :request-method :post
                                                         :content-type "application/x-www-form-urlencoded"
                                                         :body "username=somename&email-address=test%40email.address.com"))]
          sign-in-with-refer-response => (helpers/check-redirects-to target-uri)))

  (fact "unauthorised, registered user can sign in and be referred to a target uri"
        (against-background
          (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
          (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                     :result {:_id USER_ID}}
          (utils/safen-url "/target") => "/target")
        (let [target-uri "/target"
              user-session (helpers/front-end-context)
              sign-in-with-refer-response (-> user-session
                                              (p/request (str utils/host-url "/sign-in?refer=" target-uri))
                                              (p/request twitter-callback-url)
                                              (p/request sign-up-url))]
          sign-in-with-refer-response => (helpers/check-redirects-to target-uri)))

  (fact "users with an unrecognized referer are sent back to /"
        (against-background
          (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
          (http-api/create-user anything) => {:status ::http-api/success
                                              :result {:_id USER_ID}}
          (utils/safen-url "/danger-zone") => nil)
        (let [target-uri "/danger-zone"
              user-session (helpers/front-end-context)
              sign-in-with-refer-response (-> user-session
                                              (p/request (str utils/host-url "/sign-in?refer=" target-uri))
                                              (p/request twitter-callback-url)
                                              (p/request sign-up-url :request-method :post
                                                         :content-type "application/x-www-form-urlencoded"
                                                         :body "username=somename&email-address=test%40email.address.com"))]
          sign-in-with-refer-response => (helpers/check-redirects-to "/" 303))))
