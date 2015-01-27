(ns d-cent.workflows.twitter-test
  (:require [d-cent.workflows.twitter :refer :all]
            [midje.sweet :refer :all]
            [oauth.client :as oauth]
            [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]))

(def fake-request {})

(defn with-verifier [request]
  (assoc-in request [:params :oauth_verifier] "verifier"))

;;TODO - inject uri and token providers into workflow?
;;TODO - check that routes are wired up correctly
;;TODO - Work through failure conditions for twitter sign-in flow

(facts "The first step"
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

;; (fact "The second step is handled by twitter")

(facts "The final step"
       (fact "authenticates the user with the :signed-in role"
             (twitter-callback (-> fake-request with-verifier)) => :an-authentication-map
             (provided 
              (workflows/make-auth {:username "user id" :roles #{:signed-in}}
                                   {::friend/workflow :d-cent.workflows.twitter/twitter-workflow}) 
              => :an-authentication-map
              
              (oauth/access-token consumer anything "verifier")
              => {:user_id "user id"})))
