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
    (rendered-response "index.mustache"
                       {:doc-title (t' :index/doc-title)
                        :doc-description (t' :index/doc-description)
                        :objective-create-btn-text (t' :index/objective-create-btn-text)
                        :twitter-sign-in (t' :index/twitter-sign-in)
                        :signed-in (when username true)
                        :username username
                        :locale (subs (str locale) 1)})))

(defn sign-in [{:keys [t' locale]}]
  (rendered-response "sign_in.mustache"
                     {:doc-title (t' :sign-in/doc-title)
                      :doc-description (t' :sign-in/doc-description)
                      :twitter-sign-in (t' :sign-in/twitter-sign-in)
                      :locale (subs (str locale) 1)}))

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
  (rendered-response "objective_link.mustache"
                     {:objective-link (new-objective-link stored-objective)
                      :objective-link-text (t' :objective-new-link/objective-link-text)
                      :locale (subs (str locale) 1)}))

(defn objective-create [{:keys [t' locale] :as request}]
  (rendered-response "objective_create.mustache"
                     {:doc-title (t' :objective-create/doc-title)
                      :doc-description (t' :objective-create/doc-description)
                      :page-title (t' :objective-create/page-title)
                      :title-label (t' :objective-create/title-label)
                      :description-label (t' :objective-create/description-label)
                      :actions-label (t' :objective-create/actions-label)
                      :end-date-label (t' :objective-create/end-date)
                      :submit (t' :objective-create/submit)
                      :locale (subs (str locale) 1)}))

(defn objective-create-post [request]
  (let [objective (request->objective request)]
    (if objective
      (let [stored-objective (storage/store! "d-cent-test" objective)]
        (objective-new-link-page request stored-objective))
      (simple-response "oops"))))

(defn objective-view [{:keys [t' locale] :as request}]
  (let [objective (storage/retrieve "d-cent-test" (-> request :route-params :id))]
    (rendered-response "objective_view.mustache"
                       {:doc-title (t' :objective-view/doc-title)
                        :doc-description (t' :objective-view/doc-description)
                        :title (:title objective)
                        :actions-label (t' :objective-view/actions-label)
                        :actions (:actions objective)
                        :end-date-label (t' :objective-view/end-date-label)
                        :end-date (:end-date objective)
                        :description-label (t' :objective-view/description-label)
                        :description (:description objective)
                        :locale (subs (str locale) 1)
                        } )))

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
