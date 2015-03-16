(ns objective8.unit.workflows.twitter-test
  (:require [objective8.workflows.twitter :refer :all]
            [midje.sweet :refer :all]
            [oauth.client :as oauth]
            [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]))

(def fake-request {})

(defn with-verifier [request]
  (assoc-in request [:params :oauth_verifier] "verifier"))

(facts "step 1: obtaining request token"
       (fact "Sends user to twitter approval page with correct oauth token"
             (twitter-sign-in fake-request)
             => (contains {:status 302
                           :headers {"Location" "https://api.twitter.com/oauth/authenticate?oauth_token=wibble"}})
             (provided
              (oauth/request-token consumer callback-url)
              => {:oauth_token "wibble"}))

       (fact "Returns a Bad Gateway error if twitter response indicates an error"
             (twitter-sign-in fake-request) => (contains {:status 502})
             (provided
              (oauth/request-token consumer callback-url)
              =throws=> (ex-info "blah" {}))))

;; (fact "step 2: handled by twitter")

(facts "step 3: authenticating user"
       (fact "stores the twitter user id in the session and redirects to capture-profile"
             (against-background
              (oauth/access-token consumer anything "verifier")
              => {:user_id "user-id"})

             (let [response (twitter-callback (-> fake-request with-verifier))]
               (:session response) => (contains {:twitter-id "twitter-user-id"})
               response => (contains {:status 302})
               (:headers response) => (contains {"Location" (contains "sign-up")})))

       (fact "redirects to homepage when user doesn't authorise application or error in twitter"
             (against-background
              (oauth/access-token anything anything anything)
              =throws=> (ex-info "blah" {}))

             (twitter-callback fake-request) => (contains {:status 302})
             (:headers (twitter-callback fake-request)) => (contains {"Location" (contains "/")})))
