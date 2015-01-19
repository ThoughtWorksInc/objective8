(ns d-cent.workflows.twitter-test
  (:require [d-cent.workflows.twitter :refer :all]
            [midje.sweet :refer :all]
            [oauth.client :as oauth]))

(def fake-request {})

(defn with-verifier [request]
  (assoc-in request [:params :oauth_verifier] "verifier"))

;;TODO - inject uri and token providers into workflow?

(fact "Sends user to twitter approval page with correct oauth token"
      (twitter-sign-in fake-request) => (contains {:status 302
                                                 :headers {"Location" "https://api.twitter.com/oauth/authenticate?oauth_token=wibble"}})
      (provided
        (oauth/request-token consumer callback-url) => {:oauth_token "wibble"}))

(fact "Returns an error if the oauth token is unavailable"
      (twitter-sign-in fake-request) => (contains {:status 502})
      (provided
        (oauth/request-token consumer callback-url) => nil))

(fact "Sets session username to twitter screen name"
      (twitter-callback (-> fake-request with-verifier)) => (contains {:username "screen name"})
      (provided
       (oauth/access-token consumer anything "verifier") => {:screen_name "screen name"}) )










