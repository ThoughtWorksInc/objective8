(ns d-cent.workflows.twitter
    (:require [oauth.client :as oauth]
              [ring.util.response :as response]
              [bidi.ring :refer [make-handler]]
              [cemerick.friend :as friend]
              [cemerick.friend.workflows :as workflows]
              [d-cent.config :as config]))

(def consumer (oauth/make-consumer config/twitter-consumer-token
                                   config/twitter-consumer-secret-token
                                   "https://api.twitter.com/oauth/request_token"
                                   "https://api.twitter.com/oauth/access_token"
                                   "https://api.twitter.com/oauth/authenticate"
                                   :hmac-sha1))

(def twitter-routes
  ["/" {"twitter-login"    :login
        "twitter-callback" :callback}])

(defn twitter-login [_]
  (let [request-token (oauth/request-token consumer "http://localhost:8080/twitter-callback")
        approval-uri (oauth/user-approval-uri consumer (:oauth_token request-token))]
    (response/redirect approval-uri)))

(defn twitter-callback [{params :params}]
  (let [access-token-response (oauth/access-token consumer
                                                  params
                                                  (:oauth_verifier params))]
    (friend/merge-authentication
     (response/redirect "/")
     (workflows/make-auth {:username (access-token-response :screen_name)}
                          {::friend/workflow ::twitter-workflow}))))

(def twitter-handlers
  {:login    twitter-login
   :callback twitter-callback})

(def twitter-workflow
  (make-handler twitter-routes twitter-handlers))

