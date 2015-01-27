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
  (try
    (let [request-token-response (oauth/request-token consumer callback-url)
          approval-uri (oauth/user-approval-uri consumer (:oauth_token request-token-response))]
      (response/redirect approval-uri))
    (catch clojure.lang.ExceptionInfo e 
      (do (log/warn (str "Could not get request token from twitter: " e))
          {:status 502}))))

(defn twitter-callback [{params :params}]
  (try 
    (let [twitter-response (oauth/access-token consumer
                                               params
                                               (:oauth_verifier params))]
      (workflows/make-auth {:username (twitter-response :user_id) :roles #{:signed-in}}
                           {::friend/workflow ::twitter-workflow}))
    (catch clojure.lang.ExceptionInfo e
      (do (log/info (str "Did not get authentication from twitter: " e)) 
          (response/redirect "/")))))

(def twitter-handlers
  {:sign-in       twitter-sign-in
   :callback      twitter-callback})

(def twitter-workflow
  (make-handler twitter-routes twitter-handlers))
