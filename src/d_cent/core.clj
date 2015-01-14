(ns d-cent.core
  (:use [org.httpkit.server :only [run-server]])
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as response]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
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

(defn index [{:keys [t' locale]}]
  (rendered-response "index.mustache"
                     {:title (t' :index/title)
                      :welcome (t' :index/welcome)
                      :locale (subs (str locale) 1)}))

(defn login [_]
  (let [request-token (oauth/request-token consumer "http://localhost:8080/twitter-callback")
        approval-uri (oauth/user-approval-uri consumer (:oauth_token request-token))]
    (response/redirect approval-uri)))

(defn twitter-callback [{params :params}]
  (let [access-token-response (oauth/access-token consumer
                                                  params
                                                  (:oauth_verifier params))]
    (simple-response (str access-token-response))))

(def handlers {:index (wrap-tower index translation-config)
               :login login
               :twitter-callback (wrap-params (wrap-keyword-params twitter-callback))})

(def app
  (make-handler ["/" {""                 :index
                      "login"            :login
                      "twitter-callback" :twitter-callback
                      "static/"          (->Resources {:prefix "public/"})}]
                ;; We need this filtering to make the static handler work
                (some-fn handlers #(when (fn? %) %))))

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
