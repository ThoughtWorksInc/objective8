(ns d-cent.workflows.twitter
    (:require [clojure.tools.logging :as log]
              [oauth.client :as oauth]
              [ring.util.response :as response]
              [ring.util.request :as request]
              [bidi.ring :refer [make-handler]]
              [cemerick.friend :as friend]
              [cemerick.friend.workflows :as workflows]
              [d-cent.responses :refer [simple-response]]
              [d-cent.config :as config]))

(def consumer (oauth/make-consumer (config/get-var "TWITTER_CONSUMER_TOKEN")
                                   (config/get-var "TWITTER_CONSUMER_SECRET_TOKEN")
                                   "https://api.twitter.com/oauth/request_token"
                                   "https://api.twitter.com/oauth/access_token"
                                   "https://api.twitter.com/oauth/authenticate"
                                   :hmac-sha1))

(def callback-url (str "http://"
                       (config/get-var "BASE_URI" "localhost")
                       ":"
                       (config/get-var "PORT" "8080")
                       "/twitter-callback"))

(def twitter-routes
  ["/" {"twitter-sign-in"  :sign-in
        "twitter-callback" :callback}])

(defn twitter-sign-in [request]
  (let [request-token (oauth/request-token consumer callback-url)
        approval-uri (when-let [oauth-token (:oauth_token request-token)]
                       (oauth/user-approval-uri consumer oauth-token))]
    (if approval-uri
      (response/redirect approval-uri)
      (response/status (response/response "Something went wrong with twitter") 502))))

(defn twitter-callback [{params :params}]
  (let [twitter-response (oauth/access-token consumer
                                             params
                                             (:oauth_verifier params))]
    (workflows/make-auth {:username (twitter-response :user_id) :roles #{:signed-in}}
                         {::friend/workflow ::twitter-workflow})))

(def twitter-handlers
  {:sign-in       twitter-sign-in
   :callback      twitter-callback})

(def twitter-workflow
  (make-handler twitter-routes twitter-handlers))
