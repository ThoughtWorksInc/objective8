(ns d-cent.core
  (:use [org.httpkit.server :only [run-server]])
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.flash :refer [wrap-flash]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [bidi.ring :refer [make-handler ->Resources]]
            [taoensso.tower.ring :refer [wrap-tower]]
            [d-cent.config :as config]
            [d-cent.translation :refer [translation-config]]
            [d-cent.storage :as storage]
            [d-cent.workflows.twitter :refer [twitter-workflow]]
            [d-cent.workflows.profile :refer [capture-profile-workflow]]
            [d-cent.handlers.api :as api-handlers]
            [d-cent.handlers.front-end :as front-end-handlers])
  (:gen-class))

;; Custom ring middleware

(defn wrap-api-authorize [handler roles]
  (fn [request]
    (if (friend/authorized? roles friend/*identity*)
      (handler request)
      {:status 401})))

(defn inject-db [handler store]
  (fn [request] (handler (assoc request :d-cent {:store store}))))

(def handlers {; Front End Handlers
               :index front-end-handlers/index
               :sign-in front-end-handlers/sign-in
               :sign-out front-end-handlers/sign-out
               :create-user-profile-form  (friend/wrap-authorize front-end-handlers/create-user-profile-form #{:signed-in})
               :create-user-profile-form-post (friend/wrap-authorize front-end-handlers/create-user-profile-form-post #{:signed-in})
               :create-objective-form (friend/wrap-authorize front-end-handlers/create-objective-form #{:signed-in})
               :create-objective-form-post (friend/wrap-authorize front-end-handlers/create-objective-form-post #{:signed-in})
               :objective front-end-handlers/objective
               ; API Handlers
               :post-user-profile api-handlers/post-user-profile
               :post-objective api-handlers/post-objective
               :get-objective api-handlers/get-objective})

(def routes
  ["/" {""                  :index

        "sign-in"           :sign-in

        "sign-out"          :sign-out

        "email"             {:get :create-user-profile-form}

        "users"             {:post :create-user-profile-form-post}

        "static/"           (->Resources {:prefix "public/"})

        "objectives"        {:post :create-objective-form-post
                             ["/create"] :create-objective-form
                             ["/" :id] :objective }

        "api/v1"            {"/users" {:post :post-user-profile}

                            "/objectives" {:post :post-objective
                                          ["/" :id] :get-objective}}}

   ])

(defn app [app-config]
  (-> (make-handler routes (some-fn handlers #(when (fn? %) %)))
      (friend/authenticate (:authentication app-config))
      (wrap-tower (:translation app-config))
      wrap-keyword-params
      wrap-params
      wrap-json-params
      wrap-json-response
      wrap-flash
      wrap-session
      (inject-db (:store app-config))))

(defonce server (atom nil))
(defonce in-memory-db (atom {}))

(def app-config
  {:authentication {:allow-anon? true
                    :workflows [twitter-workflow,
                                capture-profile-workflow]
                    :login-uri "/sign-in"}
   :translation translation-config
   :store in-memory-db})

(defn start-server []
  (let [port (Integer/parseInt (config/get-var "PORT" "8080"))]
    (log/info (str "Starting d-cent on port " port))
    (reset! server (run-server (app app-config) {:port port}))))

(defn -main []
  (start-server))

(defn stop-server []
  (when-not (nil? @server)
    (@server)
    (reset! server nil)))

(defn restart-server []
  (stop-server)
  (start-server))
