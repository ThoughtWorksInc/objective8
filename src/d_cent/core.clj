(ns d-cent.core
  (:use [org.httpkit.server :only [run-server]])
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [cemerick.friend.credentials :as creds]
            [ring.util.response :as response]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [bidi.ring :refer [make-handler ->Resources]]
            [taoensso.tower.ring :refer [wrap-tower]]
            [oauth.client :as oauth]
            [d-cent.responses :refer :all]
            [d-cent.translation :refer [translation-config]]))

(defonce server (atom nil))

(def environment (System/getenv))

(def twitter-consumer-token (get environment "TWITTER_CONSUMER_TOKEN"))
(def twitter-consumer-secret-token (get environment "TWITTER_CONSUMER_SECRET_TOKEN"))

(def consumer (oauth/make-consumer twitter-consumer-token
                                   twitter-consumer-secret-token
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
    (log/info "Twitter login")
    (response/redirect approval-uri)))

(defn twitter-callback [{params :params}]
  (let [access-token-response (oauth/access-token consumer
                                                  params
                                                  (:oauth_verifier params))]
    (log/info "Twitter callback")
    (friend/merge-authentication
     (response/redirect "/")
     (workflows/make-auth {:username (access-token-response :screen_name)}
                          {::friend/workflow ::twitter-workflow}))))

(def twitter-handlers 
  {:login    twitter-login
   :callback twitter-callback})


(def twitter-workflow
  (make-handler twitter-routes twitter-handlers))


(defn index [{:keys [t' locale]}]
  (rendered-response "index.mustache"
                     {:title (t' :index/title)
                      :welcome (t' :index/welcome)
                      :twitter-login (t' :index/twitter-login)
                      :locale (subs (str locale) 1)}))

(def handlers {:index index})

(def routes
  ["/" {""                 :index
        "static/"          (->Resources {:prefix "public/"})}])

(defn wrap-core-middleware [handler]
  (-> handler
      (friend/authenticate {:allow-anon? true
                            :workflows [twitter-workflow]})
      (wrap-tower translation-config)
      wrap-keyword-params
      wrap-params
      wrap-session))

(def app
  (wrap-core-middleware
   (make-handler routes (some-fn handlers #(when (fn? %) %)))))

(defn start-server []
  (let [port (Integer/parseInt (get environment "PORT" "8080"))]
    (log/info (str "Starting d-cent on port " port))
    (reset! server (run-server app {:port port}))))

(defn -main []
  (start-server))

(defn stop-server []
  (when-not (nil? @server)
    (@server)
    (reset! server nil)))

(defn restart-server []
  (stop-server)
  (start-server))
