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
            [ring.middleware.ssl :refer [wrap-ssl-redirect wrap-hsts wrap-forwarded-scheme]]
            [bidi.ring :refer [make-handler]]
            [taoensso.tower.ring :refer [wrap-tower]]
            [objective8.routes :as routes]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.translation :refer [configure-translations]]
            [objective8.storage.storage :as storage]
            [objective8.storage.database :as db]
            [objective8.workflows.twitter :refer [twitter-workflow]]
            [objective8.scheduler :as scheduler]
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
               :fe/add-a-question (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/add-a-question) #{:signed-in}) 
               :fe/add-question-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/add-question-form-post) #{:signed-in})
               :fe/question-list (utils/anti-forgery-hook front-end-handlers/question-list)
               :fe/question (utils/anti-forgery-hook front-end-handlers/question-detail)
               :fe/add-answer-form-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/add-answer-form-post) #{:signed-in})
               :fe/candidate-list (utils/anti-forgery-hook front-end-handlers/candidate-list)
               :fe/invite-writer (m/wrap-authorise-writer-inviter (utils/anti-forgery-hook front-end-handlers/invite-writer)) 
               :fe/invitation-form-post (m/wrap-authorise-writer-inviter (utils/anti-forgery-hook front-end-handlers/invitation-form-post))
               :fe/writer-invitation front-end-handlers/writer-invitation
               :fe/create-profile-get (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-profile-get) #{:signed-in})
               :fe/create-profile-post (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/create-profile-post) #{:signed-in})
               :fe/accept-invitation (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/accept-invitation) #{:signed-in})
               :fe/decline-invitation (utils/anti-forgery-hook front-end-handlers/decline-invitation) 
               :fe/add-draft-get (m/wrap-authorise-writer (utils/anti-forgery-hook front-end-handlers/add-draft-get))
               :fe/add-draft-post (m/wrap-authorise-writer (utils/anti-forgery-hook front-end-handlers/add-draft-post))
               :fe/draft (utils/anti-forgery-hook front-end-handlers/draft)
               :fe/draft-list front-end-handlers/draft-list
               :fe/import-draft-get (m/wrap-authorise-writer (utils/anti-forgery-hook front-end-handlers/import-draft-get)) 
               :fe/import-draft-post (m/wrap-authorise-writer (utils/anti-forgery-hook front-end-handlers/import-draft-post))
               
               :fe/post-up-vote (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/post-up-vote) #{:signed-in}) 
               :fe/post-down-vote (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/post-down-vote) #{:signed-in}) 
               :fe/post-comment (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/post-comment) #{:signed-in})
               :fe/post-star (friend/wrap-authorize (utils/anti-forgery-hook front-end-handlers/post-star) #{:signed-in})
               :fe/post-mark (m/authorize-based-on-request (utils/anti-forgery-hook front-end-handlers/post-mark) utils/can-mark-question?)

               
               ;; API Handlers
               :api/post-user-profile (m/wrap-bearer-token api-handlers/post-user-profile bt/token-provider)
               :api/get-user-by-query (m/wrap-bearer-token api-handlers/find-user-by-query bt/token-provider)
               :api/get-user (m/wrap-bearer-token api-handlers/get-user bt/token-provider)
               :api/post-writer-profile (m/wrap-bearer-token api-handlers/post-writer-profile bt/token-provider)
               :api/post-objective (m/wrap-bearer-token api-handlers/post-objective bt/token-provider)
               :api/get-objective (m/wrap-bearer-token api-handlers/get-objective bt/token-provider) 
               :api/get-objectives (m/wrap-bearer-token api-handlers/get-objectives bt/token-provider) 
               :api/post-comment (m/wrap-bearer-token api-handlers/post-comment bt/token-provider)
               :api/get-comments api-handlers/get-comments
               :api/post-question (m/wrap-bearer-token api-handlers/post-question bt/token-provider)
               :api/get-question api-handlers/get-question
               :api/get-questions-for-objective api-handlers/retrieve-questions
               :api/get-answers-for-question api-handlers/get-answers
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
               :api/post-star (m/wrap-bearer-token api-handlers/post-star bt/token-provider)
               :api/post-mark (m/wrap-bearer-token api-handlers/post-mark bt/token-provider)})
  
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
      (wrap-frame-options :sameorigin)
      ((if (config/get-var "HTTPS_ONLY")
         (comp wrap-forwarded-scheme wrap-ssl-redirect)
         identity))
      ))

(defonce server (atom nil))
(defonce scheduler (atom nil))

(def app-config
  {:authentication {:allow-anon? true
                    :workflows [twitter-workflow,
                                sign-up-workflow]
                    :login-uri "/sign-in"}
   :translation (configure-translations)
   :db-spec db/postgres-spec})

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

(defn start-scheduler []
  (reset! scheduler 
          (scheduler/start-chime (Integer/parseInt (config/get-var "SCHEDULER_INTERVAL_MINUTES" "10"))))
  (prn "Starting scheduler"))

(defn stop-scheduler []
  (when @scheduler
    (do (@scheduler)
        (prn "Stopping scheduler"))))

(defn start-server 
  ([]
   (start-server app-config)) 
  ([app-config] 
   (let [port (Integer/parseInt (config/get-var "APP_PORT" "8080"))]
     (db/connect! (:db-spec app-config)) 
     (start-scheduler)
     (initialise-api)
     (log/info (str "Starting objective8 on port " port))
     (reset! server (run-server (app app-config) {:port port})))))

(defn -main []
  (start-server))

(defn stop-server []
  (when-not (nil? @server)
    (log/info "Stopping objective8")
    (stop-scheduler)
    (@server)
    (reset! server nil)))

(defn restart-server []
  (stop-server)
  (start-server))
