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
            [d-cent.objectives :refer [request->objective]]
            [d-cent.responses :refer :all]
            [d-cent.translation :refer [translation-config]]
            [d-cent.storage :as storage]
            [d-cent.workflows.twitter :refer [twitter-workflow]]))

(defn index [{:keys [t' locale]}]
  (let [username (get (friend/current-authentication) :username)]
  (rendered-response index-page {:translation t'
                                 :locale (subs (str locale) 1)
                                 :doc-title (t' :index/doc-title)
                                 :doc-description (t' :index/doc-description)
                                 :signed-in (when username true)})))



(defn sign-in [{:keys [t' locale]}]
  (let [username (get (friend/current-authentication) :username)]
  (rendered-response sign-in-page {:translation t'
                                   :locale (subs (str locale) 1)
                                   :doc-title (t' :sign-in/doc-title)
                                   :doc-description (t' :sign-in/doc-description)
                                   :signed-in (when username true)})))

(defn sign-out [_]
  (friend/logout* (response/redirect "/")))

(defn email-capture-get [_]
  (simple-response "blah"))

(defn email-capture-post [request]
  (if (friend/authorized? #{:signed-in} friend/*identity*)
    {:status 200}
    {:status 401}))

(defn new-objective-link [stored-objective]
  (str "http://localhost:8080/objectives/" (:_id stored-objective)))


(defn objective-new-link-page [{:keys [t' locale]} stored-objective]
  (let [username (get (friend/current-authentication) :username)]
  (rendered-response objectives-new-link-page {:translation t'
                                               :locale (subs (str locale) 1)
                                               :doc-title (t' :objective-new-link/doc-title)
                                               :doc-description (t' :objective-new-link/doc-description)
                                               :stored-objective (new-objective-link stored-objective)
                                               :signed-in (when username true)})))


(defn objective-create [{:keys [t' locale]}]
  (let [username (get (friend/current-authentication) :username)]
  (rendered-response objective-create-page {:translation t'
                                            :locale (subs (str locale) 1)
                                            :doc-title (t' :objective-create/doc-title)
                                            :doc-description (t' :objective-create/doc-description)
                                            :signed-in (when username true)})))

(defn objective-create-post [request]
  (let [objective (request->objective request)]
    (if objective
      (let [stored-objective (storage/store! "d-cent-test" objective)]
        (objective-new-link-page request stored-objective))
      (simple-response "oops"))))


(defn objective-view [{:keys [t' locale] :as request}]
  (let [username (get (friend/current-authentication) :username)
        objective (storage/retrieve "d-cent-test" (-> request :route-params :id))]
    (rendered-response objective-view-page {:translation t'
                                            :locale (subs (str locale) 1)
                                            :doc-title (t' :objective-view/doc-title)
                                            :doc-description (t' :objective-view/doc-description)
                                            :objective objective
                                            :signed-in (when username true)})))

(def handlers {:index index
               :sign-in sign-in
               :sign-out sign-out
               :email-capture-get    (friend/wrap-authorize email-capture-get #{:signed-in})
               :email-capture-post email-capture-post
               :objective-create (friend/wrap-authorize objective-create #{:signed-in})
               :objective-create-post (-> objective-create-post wrap-keyword-params wrap-params)
               :objective-view objective-view })

(def routes
  ["/" {""                  :index
        "sign-in"           :sign-in
        "sign-out"          :sign-out
        "email"             {:get :email-capture-get
                             :post :email-capture-post}
        "static/"           (->Resources {:prefix "public/"})
        "objectives"        {["/create"] :objective-create
                             :post :objective-create-post
                             ["/" :id] :objective-view }}])

(defn wrap-core-middleware [handler]
  (-> handler
      wrap-keyword-params
      wrap-params
      wrap-session))

(def app
  (-> (make-handler routes (some-fn handlers #(when (fn? %) %)))
      (friend/authenticate {:allow-anon? true
                            :workflows [twitter-workflow]
                            :login-uri "/sign-in"})
      (wrap-tower translation-config)))

(defonce server (atom nil))

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
