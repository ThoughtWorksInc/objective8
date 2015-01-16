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

(def callback-url "http://localhost:8080/twitter-callback")

(def twitter-routes
  ["/" {"twitter-login"    :login
        "twitter-callback" :callback}])

(defn twitter-login [_]
  (let [request-token (oauth/request-token consumer callback-url)
        approval-uri (when-let [oauth-token (:oauth_token request-token)]
                       (oauth/user-approval-uri consumer oauth-token))]
    (if approval-uri
      (response/redirect approval-uri)
      (response/status (response/response "Something went wrong with twitter") 502))))

(defn twitter-callback [{params :params}]
  (let [access-token-response (oauth/access-token consumer
                                                  params
                                                  (:oauth_verifier params))]
    (workflows/make-auth {:username (access-token-response :screen_name) :roles #{:logged-in}}
                         {::friend/workflow ::twitter-workflow})))

(def twitter-handlers
  {:login    twitter-login
   :callback twitter-callback})

(def twitter-workflow
  (make-handler twitter-routes twitter-handlers))
