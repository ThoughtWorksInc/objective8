(ns d-cent.core
  (:use [org.httpkit.server :only [run-server]])
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [org.httpkit.client :as http]
            [ring.util.response :as response]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response wrap-json-body]]
            [bidi.ring :refer [make-handler ->Resources]]
            [taoensso.tower.ring :refer [wrap-tower]]
            [d-cent.config :as config]
            [d-cent.objectives :refer [request->objective]]
            [d-cent.responses :refer :all]
            [d-cent.translation :refer [translation-config]]
            [d-cent.user :as user]
            [d-cent.storage :as storage]
            [d-cent.utils :as utils]
            [d-cent.workflows.twitter :refer [twitter-workflow]]
            [cheshire.core :as json]))

;; Custom ring middleware

(defn wrap-api-authorize [handler roles]
  (fn [request]
    (if (friend/authorized? roles friend/*identity*)
      (handler request)
      {:status 401})))

(defn inject-db [handler store]
  (fn [request] (handler (assoc request :d-cent {:store store}))))


;; Helpers

(defn signed-in? []
  (friend/authorized? #{:signed-in} friend/*identity*))


;; Handlers

(defn index [{:keys [t' locale]}]
  (rendered-response index-page {:translation t'
                                 :locale (subs (str locale) 1)
                                 :doc-title (t' :index/doc-title)
                                 :doc-description (t' :index/doc-description)
                                 :signed-in (signed-in?)}))

(defn sign-in [{:keys [t' locale]}]
  (rendered-response sign-in-page {:translation t'
                                   :locale (subs (str locale) 1)
                                   :doc-title (t' :sign-in/doc-title)
                                   :doc-description (t' :sign-in/doc-description)
                                   :signed-in (signed-in?)}))

(defn sign-out [_]
  (friend/logout* (response/redirect "/")))

(defn email-capture-get [{:keys [t' locale]}]
  (rendered-response users-email {:translation t'
                                  :locale (subs (str locale) 1)
                                  :doc-title (t' :users-email/doc-title)
                                  :doc-description (t' :users-email/doc-description)
                                  :signed-in (signed-in?)}))

(defn user-profile-post [request]
  (println "-------------- current authentication ---------------")
  (println (str (friend/current-authentication)))
  (let [user-id (:username (friend/current-authentication))
        email-address (get-in request [:params :email-address])
        api-response @(http/post "http://localhost:8080/api/v1/users" 
                                 {:headers {"Content-Type" "application/json"} 
                                  :body (json/generate-string {:user-id user-id 
                                                               :email-address email-address})})]
    ; generate redirect response
    {:status 200 :body api-response}))

(defn api-user-profile-post [request]
  (log/info (:params request))
  (let [store (storage/request->store request)
        user-id (get-in request [:params :user-id])
        email-address (get-in request [:params :email-address])]
    (user/store-user-profile! store {:user-id user-id :email-address email-address})
    {:status 200}))

(defn new-objective-link [stored-objective]
  (str utils/host-url "/objectives/" (:_id stored-objective)))

(defn objective-new-link-page [{:keys [t' locale]} stored-objective]
  (rendered-response objectives-new-link-page {:status-code 201
                                               :translation t'
                                               :locale (subs (str locale) 1)
                                               :doc-title (t' :objective-new-link/doc-title)
                                               :doc-description (t' :objective-new-link/doc-description)
                                               :stored-objective (new-objective-link stored-objective)
                                               :signed-in (signed-in?)}))

(defn objective-create [{:keys [t' locale]}]
  (rendered-response objective-create-page {:translation t'
                                            :locale (subs (str locale) 1)
                                            :doc-title (t' :objective-create/doc-title)
                                            :doc-description (t' :objective-create/doc-description)
                                            :signed-in (signed-in?)}))

(defn objective-create-post [request]
  (if (friend/authorized? #{:signed-in} friend/*identity*)
    (let [objective (request->objective request)]
      (if objective
        (let [stored-objective (storage/store! (storage/request->store request) "objectives" objective)]
          (objective-new-link-page request stored-objective))
        {:status 400
         :body "oops"}))
    {:status 401}))

(defn api-objective-post []
  )

(defn objective-view [{:keys [t' locale] :as request}]
  (let [objective (storage/find-by (storage/request->store request) "objectives" (-> request :route-params :id))]
    (rendered-response objective-view-page {:translation t'
                                            :locale (subs (str locale) 1)
                                            :doc-title (t' :objective-view/doc-title)
                                            :doc-description (t' :objective-view/doc-description)
                                            :objective objective
                                            :signed-in (signed-in?)})))

(def handlers {:index index
               :sign-in sign-in
               :sign-out sign-out
               :email-capture-get  (friend/wrap-authorize email-capture-get #{:signed-in})
               :user-profile-post (friend/wrap-authorize user-profile-post #{:signed-in})
               :api-user-profile-post api-user-profile-post
               :objective-create (friend/wrap-authorize objective-create #{:signed-in})
               :objective-create-post objective-create-post
               :objective-view objective-view
               :api-objective-post api-objective-post})

(def routes
  ["/" {""                  :index
        "sign-in"           :sign-in
        "sign-out"          :sign-out
        "email"             {:get :email-capture-get}
        "users"             {:post :user-profile-post}
        "api/v1/users"      {:post :api-user-profile-post}
        "static/"           (->Resources {:prefix "public/"})
        "objectives"        {["/create"] :objective-create
                             :post :objective-create-post
                             ["/" :id] :objective-view }}
        "api/v1/objectives"  :post :api-objective-post
                              ["/" :id] :objective-view])

(defn app [app-config]
  (-> (make-handler routes (some-fn handlers #(when (fn? %) %)))
      (friend/authenticate (:authentication app-config))
      (wrap-tower (:translation app-config))
      wrap-keyword-params
      wrap-params
      wrap-json-params
      wrap-session
      (inject-db (:store app-config))))

(defonce server (atom nil))
(defonce in-memory-db (atom {}))

(def app-config
  {:authentication {:allow-anon? true
                    :workflows [twitter-workflow]
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
