(ns objective8.core
  (:use [org.httpkit.server :only [run-server]])
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.flash :refer [wrap-flash]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.x-headers :refer [wrap-xss-protection wrap-frame-options wrap-content-type-options]]
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
            [objective8.handlers.front-end :as front-end-handlers]
            [objective8.middleware :as m]
            [objective8.bearer-tokens :as bt])
 ; (:gen-class)
  
  )

(def handlers {; Front End Handlers
               :index front-end-handlers/index
               :sign-in front-end-handlers/sign-in
               :sign-out front-end-handlers/sign-out
               :project-status front-end-handlers/project-status
               :learn-more front-end-handlers/learn-more
               :create-objective-form (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-objective-form) #{:signed-in})
               :create-objective-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-objective-form-post) #{:signed-in})
               :objective-list front-end-handlers/objective-list
               :objective (utils/anti-forgery-hook front-end-handlers/objective-detail)
               :create-comment-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-comment-form-post) #{:signed-in})
               :add-question-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/add-question-form-post) #{:signed-in})
               :question-list (utils/anti-forgery-hook front-end-handlers/question-list)
               :question (utils/anti-forgery-hook front-end-handlers/question-detail)
               :add-answer-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/add-answer-form-post) #{:signed-in})
               :candidate-list (utils/anti-forgery-hook front-end-handlers/candidate-list)
               :invitation-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/invitation-form-post) #{:signed-in})
               :writer-invitation front-end-handlers/writer-invitation
               :accept-or-reject-invitation (utils/anti-forgery-hook front-end-handlers/accept-or-reject-invitation)
               :accept-invitation (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/accept-invitation) #{:signed-in})
               :decline-invitation (utils/anti-forgery-hook front-end-handlers/decline-invitation) 

               
               ; API Handlers
               :post-user-profile (m/wrap-bearer-token api-handlers/post-user-profile bt/token-provider)
               :get-user-by-query (m/wrap-bearer-token api-handlers/find-user-by-query bt/token-provider)
               :get-user (m/wrap-bearer-token api-handlers/get-user bt/token-provider)
               :post-objective (m/wrap-bearer-token api-handlers/post-objective bt/token-provider)
               :get-objective api-handlers/get-objective
               :get-objectives api-handlers/get-objectives
               :get-comments-for-objective api-handlers/retrieve-comments
               :post-comment (m/wrap-bearer-token api-handlers/post-comment bt/token-provider)
               :post-question (m/wrap-bearer-token api-handlers/post-question bt/token-provider)
               :get-question api-handlers/get-question
               :get-questions-for-objective api-handlers/retrieve-questions
               :get-answers-for-question api-handlers/retrieve-answers
               :post-answer (m/wrap-bearer-token api-handlers/post-answer bt/token-provider)
               :post-invitation (m/wrap-bearer-token api-handlers/post-invitation bt/token-provider)
               :get-invitation api-handlers/get-invitation
               :post-candidate-writer (m/wrap-bearer-token api-handlers/post-candidate-writer bt/token-provider)
               :put-invitation-declination (m/wrap-bearer-token api-handlers/put-invitation-declination bt/token-provider)
               :get-candidates-for-objective api-handlers/retrieve-candidates
               :post-start-drafting (m/wrap-bearer-token api-handlers/post-start-drafting bt/token-provider)})

(def routes
  [
   "/"  ;; FRONT-END
        {""                 :index
        "sign-in"           :sign-in
        "sign-out"          :sign-out
        "project-status"    :project-status
        "learn-more"        :learn-more
        "static/"           (->Resources {:prefix "public/"})
        "objectives"        {:get :objective-list
                             :post :create-objective-form-post
                             ["/create"] :create-objective-form
                             ["/" :id] {:get :objective
                                        "/writer-invitations" {:post :invitation-form-post
                                                               ["/" :i-id] {:get :accept-or-reject-invitation
                                                                            "/response" {:post :accept-invitation}
                                                                            "/decline" {:post :decline-invitation}
                                                                            }}
                                        "/candidate-writers" {:get :candidate-list}
                                        "/questions" {:post :add-question-form-post
                                                      :get :question-list
                                                      ["/" :q-id] {:get :question
                                                                   "/answers" {:post :add-answer-form-post}}}}}
        "comments"          {:post :create-comment-form-post}
        "invitations"       {["/" :uuid] {:get :writer-invitation}}

        ;; API
        "api/v1"            {"/users" {:post :post-user-profile
                                       :get :get-user-by-query
                                       ["/" :id] :get-user}

                             "/objectives" {:get :get-objectives
                                            :post :post-objective
                                            ["/" :id] {:get :get-objective
                                                       "/comments" :get-comments-for-objective
                                                       "/questions" {:post :post-question
                                                                     :get :get-questions-for-objective
                                                                     ["/" :q-id] {:get :get-question
                                                                                  "/answers" {:get :get-answers-for-question
                                                                                              :post :post-answer}}}
                                                       "/candidate-writers" {:get :get-candidates-for-objective
                                                                             :post :post-candidate-writer}
                                                       "/writer-invitations" {["/" :i-id] {:put :put-invitation-declination}}
                                                       "/writers" {"/invitations" {:post :post-invitation}}}}

                             "/comments"   {:post :post-comment}
                             "/invitations" {:get :get-invitation}}

         ;;DEV-API
         "dev/api/v1"     {["/objectives/" :id "/start-drafting"] {:post :post-start-drafting}}}])

(defn app [app-config]
  (-> (make-handler routes (some-fn handlers #(when (fn? %) %)))
      (m/wrap-not-found front-end-handlers/error-404)
      (friend/authenticate (:authentication app-config))
      (wrap-tower (:translation app-config))
      m/strip-trailing-slashes
      wrap-keyword-params
      wrap-params
      wrap-json-params
      wrap-json-response
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only true}})
      (wrap-xss-protection true {:mode :block})
      (wrap-frame-options :sameorigin)))

(defonce server (atom nil))
(defonce postgres-connection-pool (atom nil))


(def app-config
  {:authentication {:allow-anon? true
                    :workflows [twitter-workflow,
                                sign-up-workflow]
                    :login-uri "/sign-in"}
   :translation translation-config})

(defn get-bearer-token-details []
  (let [bearer-name (config/get-var "API_BEARER_NAME")
        bearer-token (config/get-var "API_BEARER_TOKEN")]
    (when (and bearer-name bearer-token)
      {:bearer-name bearer-name
       :bearer-token bearer-token})))

(defn initialise-api []
  (if-let [bearer-token-details (get-bearer-token-details)]
    (if (bt/get-token (bearer-token-details :bearer-name))
      (bt/update-token! bearer-token-details)
      (bt/store-token! bearer-token-details))))

(defn start-server []
  (let [port (Integer/parseInt (config/get-var "APP_PORT" "8080"))]
    (reset! postgres-connection-pool (db/connect! db/postgres-spec))
    (initialise-api)
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
