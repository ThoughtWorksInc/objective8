(ns d-cent.core
  (:use [org.httpkit.server :only [run-server]])
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.util.response :as response]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [bidi.ring :refer [make-handler ->Resources]]
            [taoensso.tower.ring :refer [wrap-tower]]
            [d-cent.config :as config]
            [d-cent.responses :refer :all]
            [d-cent.translation :refer [translation-config]]
            [d-cent.workflows.twitter :refer [twitter-workflow]]))

(defonce server (atom nil))

(defn index [{:keys [t' locale]}]
  (let [username (get (friend/current-authentication) :username "")]
    (rendered-response "index.mustache"
                       {:title (t' :index/title)
                        :welcome (str (t' :index/welcome) " " username)
                        :twitter-login (t' :index/twitter-login)
                        :locale (subs (str locale) 1)})))
(defn logout [_]
  (friend/logout* (response/redirect "/")))

(def handlers {:index index
               :logout logout})

(def routes
  ["/" {""                 :index
        "logout"           :logout
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
  (let [port (Integer/parseInt config/port)]
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
