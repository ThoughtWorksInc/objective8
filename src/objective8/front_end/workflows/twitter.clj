(ns objective8.front-end.workflows.twitter
    (:require [clojure.tools.logging :as log]
              [oauth.client :as oauth]
              [ring.util.response :as response]
              [ring.util.request :as request]
              [bidi.ring :refer [make-handler]]
              [objective8.front-end.views :as views]
              [objective8.utils :as utils]
              [objective8.config :as config]))

(defn configure-twitter [{:keys [consumer-token secret-token] :as twitter-credentials}]
  (if (and consumer-token secret-token)
    {:consumer (oauth/make-consumer consumer-token
                                    secret-token
                                    "https://api.twitter.com/oauth/request_token"
                                    "https://api.twitter.com/oauth/access_token"
                                    "https://api.twitter.com/oauth/authorize"
                                    :hmac-sha1)
     :callback-url (str utils/host-url "/twitter-callback")}
    :invalid-configuration))

(defn valid? [configuration]
  (not (= configuration :invalid-configuration)))

(defn twitter-sign-in [{:keys [twitter-config] :as request}]
  (try
    (let [consumer (:consumer twitter-config)
          callback-url (:callback-url twitter-config)
          _ (log/debug "Before calling oAuth request token")
          request-token-response (oauth/request-token consumer callback-url)
          _ (log/debug "After calling oAuth request token")
          approval-uri (oauth/user-approval-uri consumer (:oauth_token request-token-response))]
      (response/redirect approval-uri))
    (catch clojure.lang.ExceptionInfo e
      (do (log/warn (str "Could not get request token from twitter: " e))
          {:status 502}))))

(defn twitter-callback [{:keys [params session twitter-config] :as request}]
  (try
    (let [consumer (:consumer twitter-config)
          twitter-response (oauth/access-token consumer
                                               params
                                               (:oauth_verifier params))
          twitter-user-id (str "twitter-" (:user_id twitter-response))
          twitter-screen-name (:screen_name twitter-response)
          the-response (into (response/redirect (str utils/host-url "/sign-up"))
                             {:session (assoc session 
                                              :twitter-id twitter-user-id 
                                              :twitter-screen-name twitter-screen-name)})]
      the-response)
    (catch clojure.lang.ExceptionInfo e
      (do (log/info (str "Did not get authentication from twitter: " e))
          (response/redirect "/")))))

(defn invalid-configuration-handler [request]
  (log/info "Twitter credentials not provided.")
  (response/redirect (utils/path-for :fe/error-configuration)))

(defn wrap-twitter-config [handler twitter-config]
  (if (valid? twitter-config)
    (fn [request] (handler (assoc request :twitter-config twitter-config)))
    invalid-configuration-handler))

(def twitter-routes
  ["/" {"twitter-sign-in"  :sign-in
        "twitter-callback" :callback}])

(defn twitter-handlers [twitter-config]
  {:sign-in       (wrap-twitter-config twitter-sign-in twitter-config)
   :callback      (wrap-twitter-config twitter-callback twitter-config)})

(defn twitter-workflow [twitter-config]
  (make-handler twitter-routes (twitter-handlers twitter-config)))
