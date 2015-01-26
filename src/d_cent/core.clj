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

(defonce server (atom nil))

(defn index [{:keys [t' locale]}]
  (let [username (get (friend/current-authentication) :username)]
    (rendered-response "index.mustache"
                       {:title (t' :index/title)
                        :welcome (when username (str (t' :index/welcome) " " username))
                        :twitter-sign-in (t' :index/twitter-sign-in)
                        :signed-in (when username true)
                        :username username
                        :locale (subs (str locale) 1)})))

(defn sign-in [{:keys [t' locale]}]
  (rendered-response "sign_in.mustache"
                     {:sign-in-required-message (t' :index/sign-in-required-message)
                      :welcome (str (t' :index/welcome))
                      :twitter-sign-in (t' :index/twitter-sign-in)
                      :locale (subs (str locale) 1)}))



(defn sign-out [_]
  (friend/logout* (response/redirect "/")))

(defn new-objective-link [stored-objective]
  (str "http://localhost:8080/objectives/" (:_id stored-objective)))

(defn objective-new-link-page [{:keys [t' locale]} stored-objective]
  (rendered-response "objective_link.mustache"
                     {:objective-link (new-objective-link stored-objective)
                      :objective-link-text (t' :objective/objective-link-text)
                      :locale (subs (str locale) 1)}))


(def objective-create
  (-> (fn [{:keys [t' locale] :as request}]
        (rendered-response "objective_create.mustache"
                        {:title-label (t' :objective/title-label)
                        :description-label (t' :objective/description-label)
                        :actions-label (t' :objective/actions-label)
                        :submit (t' :objective/submit)
                        :locale (subs (str locale) 1)}))
                        (friend/wrap-authorize #{:signed-in})))

(defn objective-create-post [request]
  (let [objective (request->objective request)]
    (if objective
      (let [stored-objective (storage/store! "d-cent-test" objective)]
        (objective-new-link-page request stored-objective))
      (simple-response "oops"))))

(defn objective-get [{:keys [t' locale] :as request}]
  (let [objective (storage/retrieve "d-cent-test" (-> request :route-params :id))]
    (rendered-response "objective_view.mustache"
                       {:title-label (t' :objective/title-label)
                        :title (:title objective)
                        :description-label (t' :objective/description-label)
                        :description (:description objective)
                        :actions-label (t' :objective/actions-label)
                        :actions (:actions objective)
                        :locale (subs (str locale) 1)
                        } )))

(def handlers {:index index
               :sign-in sign-in
               :objective-create objective-create
               :sign-out sign-out
               :objective-create-post (-> objective-create-post wrap-keyword-params wrap-params)
               :objective-get objective-get })

(def routes
  ["/" {""                  :index
        "sign-in"           :sign-in
        "objectives/create" :objective-create
        "sign-out"          :sign-out
        "static/"           (->Resources {:prefix "public/"})
        "objectives"         {:post :objective-create-post
                            ["/" :id] :objective-get }}])

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
