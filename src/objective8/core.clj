(ns objective8.core
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
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.translation :refer [translation-config]]
            [objective8.storage.storage :as storage]
            [objective8.storage.database :as db]
            [objective8.workflows.twitter :refer [twitter-workflow]]
            [objective8.workflows.sign-up :refer [sign-up-workflow]]
            [objective8.handlers.api :as api-handlers]
            [objective8.handlers.front-end :as front-end-handlers])
  (:gen-class))

;; Custom ring middleware

(defn inject-db [handler store]
  (fn [request] (handler (assoc request :objective8 {:store store}))))

(def handlers {; Front End Handlers
               :index front-end-handlers/index
               :sign-in front-end-handlers/sign-in
               :sign-out front-end-handlers/sign-out
               :create-objective-form (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-objective-form) #{:signed-in})
               :create-objective-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-objective-form-post) #{:signed-in})
               :objective (utils/anti-forgery-hook front-end-handlers/objective-detail)
               :create-comment-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-comment-form-post) #{:signed-in})
               ; API Handlers
               :post-user-profile api-handlers/post-user-profile
               :find-user-by-query api-handlers/find-user-by-query
               :get-user api-handlers/get-user
               :post-objective api-handlers/post-objective
               :get-objective api-handlers/get-objective
               :post-comment api-handlers/post-comment})

(def routes
  ["/" {""                  :index

        "sign-in"           :sign-in

        "sign-out"          :sign-out

        "static/"           (->Resources {:prefix "public/"})

        "objectives"        {:post :create-objective-form-post
                             ["/create"] :create-objective-form
                             ["/" :id] :objective }

        "comments"          {:post :create-comment-form-post}

        "api/v1"            {"/users" {:post :post-user-profile
                                       :get :find-user-by-query
                                       ["/" :id] :get-user}

                            "/objectives" {:post :post-objective
                                          ["/" :id] :get-objective}

                            "/comments"   {:post :post-comment}}}

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
      wrap-session))

(defonce server (atom nil))
(defonce postgres-connection-pool (atom nil))


(def app-config
  {:authentication {:allow-anon? true
                    :workflows [twitter-workflow,
                                sign-up-workflow]
                    :login-uri "/sign-in"}
   :translation translation-config})

(defn start-server []
  (let [port (Integer/parseInt (config/get-var "APP_PORT" "8080"))]
    (reset! postgres-connection-pool (db/connect! db/postgres-spec))
    (log/info (str "Starting objective8 on port " port))
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
