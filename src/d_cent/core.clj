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
            [d-cent.proposals :refer [request->proposal]]
            [d-cent.responses :refer :all]
            [d-cent.translation :refer [translation-config]]
            [d-cent.storage :as storage]
            [d-cent.workflows.twitter :refer [twitter-workflow]]))

(defonce server (atom nil))

(defn index [{:keys [t' locale]}]
  (let [username (get (friend/current-authentication) :username "")]
    (rendered-response "index.mustache"
                       {:title (t' :index/title)
                        :welcome (str (t' :index/welcome) " " username)
                        :twitter-login (t' :index/twitter-login)
                        :locale (subs (str locale) 1)})))

(defn login [{:keys [t' locale]}]
  (rendered-response "login.mustache"
                     {:login-required-message "Please log-in"
                      :welcome (str (t' :index/welcome))
                      :twitter-login (t' :index/twitter-login)
                      :locale (subs (str locale) 1)}))


(def create-proposal
  (-> (fn [_] (rendered-response "create_proposal.mustache"))
      (friend/wrap-authorize #{:logged-in})))

(defn logout [_]
  (friend/logout* (response/redirect "/")))

(defn make-proposal [request]
  (let [proposal (request->proposal request)]
    (if proposal
      (let [stored-proposal (storage/store! "d-cent-test" proposal)]
        (simple-response (str "Your proposal is here: http:localhost:8080/proposals/" (:_id stored-proposal))))
      (simple-response "oops"))))

(defn get-proposal [request]
  (simple-response (storage/retrieve "d-cent-test" (-> request :route-params :id))))

(def handlers {:index index
               :login login
               :create-proposal create-proposal
               :logout logout
               :make-proposal (-> make-proposal wrap-keyword-params wrap-params)
               :retrieve-proposal get-proposal })

(def routes
  ["/" {""                :index
        "login"           :login
        "create-proposal" :create-proposal
        "logout"          :logout
        "static/"          (->Resources {:prefix "public/"})
        "proposals"       {:post :make-proposal
                           ["/" :id] :retrieve-proposal }}])

(defn wrap-core-middleware [handler]
  (-> handler
      wrap-keyword-params
      wrap-params
      wrap-session))

(def app
  (-> (make-handler routes (some-fn handlers #(when (fn? %) %)))
      (friend/authenticate {:allow-anon? true
                            :workflows [twitter-workflow]})
      (wrap-tower translation-config)))

(defn start-server []
  (let [port (Integer/parseInt (config/get-var "PORT" "8080"))]
    (log/info (str "Starting d-cent on port " port))
    (reset! server (run-server (wrap-core-middleware app) {:port port}))))

(defn -main []
  (start-server))

(defn stop-server []
  (when-not (nil? @server)
    (@server)
    (reset! server nil)))

(defn restart-server []
  (stop-server)
  (start-server))
