(ns objective8.unit.workflows.twitter-test
  (:require [midje.sweet :refer :all]
            [oauth.client :as oauth]
            [objective8.front-end.workflows.twitter :refer :all]))

(def consumer (oauth/make-consumer "FAKE_TOKEN"
                                   "FAKE_SECRET"
                                   "https://api.twitter.com/oauth/request_token"
                                   "https://api.twitter.com/oauth/access_token"
                                   "https://api.twitter.com/oauth/authenticate"
                                   :hmac-sha1))

(def fake-request {:twitter-config {:consumer consumer
                                    :callback-url :callback-url}})

(defn with-verifier [request]
  (assoc-in request [:params :oauth_verifier] "verifier"))

(facts "step 1: obtaining request token"
       (fact "Sends user to twitter approval page with correct oauth token"
             (twitter-sign-in fake-request)
             => (contains {:status 302
                           :headers {"Location" "https://api.twitter.com/oauth/authenticate?oauth_token=wibble"}})
             (provided
              (oauth/request-token consumer :callback-url)
              => {:oauth_token "wibble"}))

       (fact "Returns a Bad Gateway error if twitter response indicates an error"
             (twitter-sign-in fake-request) => (contains {:status 502})
             (provided
              (oauth/request-token consumer :callback-url)
              =throws=> (ex-info "blah" {}))))

;; (fact "step 2: handled by twitter")

(facts "step 3: authenticating user"
       (fact "stores the twitter user id in the session and redirects to capture-profile"
             (against-background
              (oauth/access-token consumer anything "verifier")
              => {:user_id "user-id"})

             (let [response (twitter-callback (-> fake-request with-verifier))]
               (:session response) => (contains {:auth-provider-user-id "twitter-user-id"})
               response => (contains {:status 302})
               (:headers response) => (contains {"Location" (contains "sign-up")})))

       (fact "redirects to homepage when user doesn't authorise application or error in twitter"
             (against-background
              (oauth/access-token anything anything anything)
              =throws=> (ex-info "blah" {}))

             (twitter-callback fake-request) => (contains {:status 302})
             (:headers (twitter-callback fake-request)) => (contains {"Location" (contains "/")})))

(facts "About wrap-twitter-config"
       (fact "includes the twitter configuration in the request when twitter is correctly configured"
             (let [handler (wrap-twitter-config identity :a-valid-twitter-configuration)]
               (handler {}) => {:twitter-config :a-valid-twitter-configuration}))
       
       (fact "Redirects to configuration error page when twitter is incorrectly configured"
             (let [handler (wrap-twitter-config identity :invalid-configuration)]
               (handler {}) => (contains {:status 302
                                          :headers (contains {"Location" (contains "/error/configuration")})}))))
