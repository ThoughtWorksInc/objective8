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
            [bidi.ring :refer [make-handler]]
            [taoensso.tower.ring :refer [wrap-tower]]
            [objective8.routes :as routes]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.translation :refer [configure-translations]]
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

(def handlers {;; Front End Handlers
               :fe/index front-end-handlers/index
               :fe/sign-in front-end-handlers/sign-in
               :fe/sign-out front-end-handlers/sign-out
               :fe/project-status front-end-handlers/project-status
               :fe/learn-more front-end-handlers/learn-more
               :fe/create-objective-form (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-objective-form) #{:signed-in})
               :fe/create-objective-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-objective-form-post) #{:signed-in})
               :fe/objective-list front-end-handlers/objective-list
               :fe/objective (utils/anti-forgery-hook front-end-handlers/objective-detail)
               :fe/create-comment-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-comment-form-post) #{:signed-in})
               :fe/add-question-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/add-question-form-post) #{:signed-in})
               :fe/question-list (utils/anti-forgery-hook front-end-handlers/question-list)
               :fe/question (utils/anti-forgery-hook front-end-handlers/question-detail)
               :fe/add-answer-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/add-answer-form-post) #{:signed-in})
               :fe/candidate-list (utils/anti-forgery-hook front-end-handlers/candidate-list)
               :fe/invitation-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/invitation-form-post) #{:signed-in})
               :fe/writer-invitation front-end-handlers/writer-invitation
               :fe/accept-or-decline-invitation (utils/anti-forgery-hook front-end-handlers/accept-or-decline-invitation)
               :fe/accept-invitation (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/accept-invitation) #{:signed-in})
               :fe/decline-invitation (utils/anti-forgery-hook front-end-handlers/decline-invitation) 
               :fe/add-draft-get (m/wrap-authorise-writer front-end-handlers/add-draft-get)
               :fe/add-draft-post (m/wrap-authorise-writer front-end-handlers/add-draft-post)
               :fe/draft front-end-handlers/draft-detail
               :fe/draft-list front-end-handlers/draft-list
               :fe/post-up-vote (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/post-up-vote) #{:signed-in}) 
               :fe/post-down-vote (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/post-down-vote) #{:signed-in}) 

               
               ;; API Handlers
               :api/post-user-profile (m/wrap-bearer-token api-handlers/post-user-profile bt/token-provider)
               :api/get-user-by-query (m/wrap-bearer-token api-handlers/find-user-by-query bt/token-provider)
               :api/get-user (m/wrap-bearer-token api-handlers/get-user bt/token-provider)
               :api/post-objective (m/wrap-bearer-token api-handlers/post-objective bt/token-provider)
               :api/get-objective api-handlers/get-objective
               :api/get-objectives api-handlers/get-objectives
               :api/post-comment (m/wrap-bearer-token api-handlers/post-comment bt/token-provider)
               :api/get-comments api-handlers/get-comments
               :api/post-question (m/wrap-bearer-token api-handlers/post-question bt/token-provider)
               :api/get-question api-handlers/get-question
               :api/get-questions-for-objective api-handlers/retrieve-questions
               :api/get-answers-for-question api-handlers/retrieve-answers
               :api/post-answer (m/wrap-bearer-token api-handlers/post-answer bt/token-provider)
               :api/post-invitation (m/wrap-bearer-token api-handlers/post-invitation bt/token-provider)
               :api/get-invitation api-handlers/get-invitation
               :api/post-candidate-writer (m/wrap-bearer-token api-handlers/post-candidate-writer bt/token-provider)
               :api/put-invitation-declination (m/wrap-bearer-token api-handlers/put-invitation-declination bt/token-provider)
               :api/get-candidates-for-objective api-handlers/retrieve-candidates
               :api/post-draft (m/wrap-bearer-token api-handlers/post-draft bt/token-provider)
               :api/get-draft api-handlers/get-draft
               :api/get-drafts-for-objective api-handlers/retrieve-drafts
               :api/post-up-down-vote (m/wrap-bearer-token api-handlers/post-up-down-vote bt/token-provider)

               ;; DEV API Handlers
               :dev/post-start-drafting (m/wrap-bearer-token api-handlers/post-start-drafting bt/token-provider)

               ;; Deprecated handlers - these will be removed as part of #19
               :api/post-comment-DEPRECATED (m/wrap-bearer-token api-handlers/post-comment-DEPRECATED bt/token-provider)
               :api/get-comments-for-objective api-handlers/retrieve-comments-DEPRECATED
               })

(defn app [app-config]
  (-> (make-handler routes/routes (some-fn handlers #(when (fn? %) %)))
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

(def app-config
  {:authentication {:allow-anon? true
                    :workflows [twitter-workflow,
                                sign-up-workflow]
                    :login-uri "/sign-in"}
   :translation (configure-translations)})

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

(defn start-server 
  ([] 
   (start-server app-config)) 
  ([app-config] 
   (let [port (Integer/parseInt (config/get-var "APP_PORT" "8080"))]
     (db/connect!) 
     (initialise-api)
     (log/info (str "Starting objective8 on port " port))
     (reset! server (run-server (app app-config) {:port port})))))

(defn -main []
  (start-server))

(defn stop-server []
  (when-not (nil? @server)
    (log/info "Stopping objective8")
    (@server)
    (reset! server nil)))

(defn restart-server []
  (stop-server)
  (start-server))
