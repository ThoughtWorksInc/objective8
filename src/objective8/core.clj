(ns objective8.core
  (:use [org.httpkit.server :only [run-server]])
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.memory :refer [memory-store]]
            [ring.middleware.flash :refer [wrap-flash]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.x-headers :refer [wrap-xss-protection wrap-frame-options wrap-content-type-options]]
            [ring.middleware.ssl :refer [wrap-ssl-redirect wrap-hsts wrap-forwarded-scheme]]
            [bidi.ring :refer [make-handler]]
            [taoensso.tower.ring :refer [wrap-tower]]
            [stonecutter-oauth.client :as soc]
            [objective8.routes :as routes]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.middleware :as m]
            [objective8.front-end.permissions :as permissions]
            [objective8.front-end.translation :refer [configure-translations]]
            [objective8.front-end.workflows.twitter :refer [twitter-workflow configure-twitter]]
            [objective8.front-end.workflows.facebook :refer [facebook-workflow]]
            [objective8.front-end.workflows.okta :refer [okta-workflow]]
            [objective8.front-end.workflows.stonecutter :as stonecutter]
            [objective8.front-end.workflows.stub-twitter :refer [stub-twitter-workflow]]
            [objective8.front-end.workflows.sign-up :refer [sign-up-workflow]]
            [objective8.front-end.handlers :as front-end-handlers]
            [objective8.back-end.domain.users :as users]
            [objective8.back-end.handlers :as back-end-handlers]
            [objective8.back-end.storage.database :as db]
            [objective8.back-end.domain.bearer-tokens :as bt]))

(defn all-okta-credentials-provided? []
  (let [okta-credentials (:okta-credentials config/environment)]
    (and (:client-id okta-credentials) (:client-secret okta-credentials) (:auth-url okta-credentials))))

(defn wrap-signed-in [handlers]
  (if (or (all-okta-credentials-provided?) (:private-mode-enabled config/environment))
    (m/wrap-handlers-except handlers #(friend/wrap-authorize % #{:signed-in})
                            #{:fe/error-log-in :fe/sign-in :fe/create-profile-get
                              :fe/create-profile-post :fe/authorisation-page
                              :fe/error-configuration})
    (m/wrap-just-these-handlers handlers #(friend/wrap-authorize % #{:signed-in})
                                #{:fe/create-objective-form :fe/create-objective-form-post :fe/add-a-question
                                  :fe/add-question-form-post :fe/add-answer-form-post :fe/create-profile-get
                                  :fe/create-profile-post :fe/edit-profile-get :fe/edit-profile-post :fe/accept-invitation
                                  :fe/post-up-vote :fe/post-down-vote :fe/post-comment :fe/post-annotation
                                  :fe/post-star :fe/post-writer-note})))

(defn front-end-handlers []
  (-> {:fe/index                           front-end-handlers/index
       :fe/sign-in                         front-end-handlers/sign-in
       :fe/sign-out                        front-end-handlers/sign-out
       :fe/profile                         front-end-handlers/profile
       :fe/project-status                  front-end-handlers/project-status
       :fe/learn-more                      front-end-handlers/learn-more
       :fe/admin-activity                  front-end-handlers/admin-activity
       :fe/create-objective-form           front-end-handlers/create-objective-form
       :fe/create-objective-form-post      front-end-handlers/create-objective-form-post
       :fe/objective-list                  front-end-handlers/objective-list
       :fe/objective                       front-end-handlers/objective-detail
       :fe/get-comments-for-objective      front-end-handlers/get-comments-for-objective
       :fe/add-a-question                  front-end-handlers/add-a-question
       :fe/add-question-form-post          front-end-handlers/add-question-form-post
       :fe/question-list                   front-end-handlers/question-list
       :fe/question                        front-end-handlers/question-detail
       :fe/add-answer-form-post            front-end-handlers/add-answer-form-post
       :fe/writers-list                    front-end-handlers/writers-list
       :fe/invite-writer                   front-end-handlers/invite-writer
       :fe/invitation-form-post            front-end-handlers/invitation-form-post
       :fe/writer-invitation               front-end-handlers/writer-invitation
       :fe/create-profile-get              front-end-handlers/create-profile-get
       :fe/create-profile-post             front-end-handlers/create-profile-post

       :fe/edit-profile-get                front-end-handlers/edit-profile-get
       :fe/edit-profile-post               front-end-handlers/edit-profile-post
       :fe/accept-invitation               front-end-handlers/accept-invitation
       :fe/decline-invitation              front-end-handlers/decline-invitation
       :fe/add-draft-get                   front-end-handlers/add-draft-get
       :fe/add-draft-post                  front-end-handlers/add-draft-post
       :fe/draft                           front-end-handlers/draft
       :fe/get-comments-for-draft          front-end-handlers/get-comments-for-draft
       :fe/draft-diff                      front-end-handlers/draft-diff
       :fe/draft-section                   front-end-handlers/draft-section
       :fe/draft-list                      front-end-handlers/draft-list
       :fe/import-draft-get                front-end-handlers/import-draft-get
       :fe/import-draft-post               front-end-handlers/import-draft-post
       :fe/dashboard-questions             front-end-handlers/dashboard-questions
       :fe/dashboard-comments              front-end-handlers/dashboard-comments
       :fe/dashboard-annotations           front-end-handlers/dashboard-annotations

       :fe/post-up-vote                    front-end-handlers/post-up-vote
       :fe/post-down-vote                  front-end-handlers/post-down-vote
       :fe/post-comment                    front-end-handlers/post-comment
       :fe/post-annotation                 front-end-handlers/post-annotation
       :fe/post-star                       front-end-handlers/post-star
       :fe/post-mark                       front-end-handlers/post-mark
       :fe/post-writer-note                front-end-handlers/post-writer-note
       :fe/admin-removal-confirmation-get  front-end-handlers/admin-removal-confirmation
       :fe/admin-removal-confirmation-post front-end-handlers/post-admin-removal-confirmation
       :fe/post-admin-removal              front-end-handlers/post-admin-removal
       :fe/post-promote-objective          front-end-handlers/post-promote-objective
       :fe/error-log-in                    front-end-handlers/error-log-in
       :fe/error-configuration             front-end-handlers/error-configuration
       :fe/authorisation-page              front-end-handlers/authorisation-page
       :fe/cookies                         front-end-handlers/cookies}
      (m/wrap-handlers-except utils/anti-forgery-hook #{:fe/index :fe/sign-in :fe/sign-out :fe/profile :fe/project-status
                                                        :fe/learn-more :fe/admin-activity :fe/create-objective-form
                                                        :fe/create-objective-form-post :fe/writer-invitation :fe/draft-diff
                                                        :fe/draft-list :fe/error-log-in :fe/error-configuration :fe/cookies})
      (wrap-signed-in)
      (m/wrap-just-these-handlers #(friend/wrap-authorize % #{:admin})
                                  #{:fe/admin-removal-confirmation-get :fe/admin-removal-confirmation-post :fe/post-admin-removal
                                    :fe/post-promote-objective})

      (m/wrap-just-these-handlers m/wrap-authorise-writer-inviter
                                  #{:fe/invite-writer :fe/invitation-form-post})
      (m/wrap-just-these-handlers #(m/authorize-based-on-request % permissions/request->writer-roles)
                                  #{:fe/add-draft-get :fe/add-draft-post :fe/import-draft-get :fe/import-draft-post
                                    :fe/dashboard-questions :fe/dashboard-comments :fe/dashboard-annotations})
      (m/wrap-just-these-handlers #(m/authorize-based-on-request % permissions/mark-request->mark-question-roles)
                                  #{:fe/post-mark})))

(defn back-end-handlers []
  (-> {:api/post-user-profile           back-end-handlers/post-user-profile
       :api/get-user-by-query           back-end-handlers/find-user-by-query
       :api/get-user                    back-end-handlers/get-user
       :api/put-writer-profile          back-end-handlers/put-writer-profile
       :api/post-objective              back-end-handlers/post-objective
       :api/get-objective               back-end-handlers/get-objective
       :api/get-objectives              back-end-handlers/get-objectives
       :api/post-comment                back-end-handlers/post-comment
       :api/get-comments                back-end-handlers/get-comments
       :api/post-question               back-end-handlers/post-question
       :api/get-question                back-end-handlers/get-question
       :api/get-questions-for-objective back-end-handlers/retrieve-questions
       :api/get-answers-for-question    back-end-handlers/get-answers
       :api/post-answer                 back-end-handlers/post-answer
       :api/post-invitation             back-end-handlers/post-invitation
       :api/get-invitation              back-end-handlers/get-invitation
       :api/post-writer                 back-end-handlers/post-writer
       :api/put-invitation-declination  back-end-handlers/put-invitation-declination
       :api/get-writers-for-objective   back-end-handlers/retrieve-writers
       :api/get-objectives-for-writer   back-end-handlers/get-objectives-for-writer
       :api/post-draft                  back-end-handlers/post-draft
       :api/get-draft                   back-end-handlers/get-draft
       :api/get-drafts-for-objective    back-end-handlers/retrieve-drafts
       :api/get-sections                back-end-handlers/get-sections
       :api/get-section                 back-end-handlers/get-section
       :api/get-annotations             back-end-handlers/get-annotations
       :api/put-promote-objective       back-end-handlers/post-promote-objective
       :api/post-admin-removal          back-end-handlers/post-admin-removal
       :api/get-admin-removals          back-end-handlers/get-admin-removals
       :api/post-up-down-vote           back-end-handlers/post-up-down-vote
       :api/post-star                   back-end-handlers/post-star
       :api/post-mark                   back-end-handlers/post-mark
       :api/post-writer-note            back-end-handlers/post-writer-note}
      (m/wrap-handlers-except #(m/wrap-bearer-token % bt/token-provider)
                              #{:api/get-comments :api/get-question :api/get-questions-for-objective
                                :api/get-answers-for-question :api/get-invitation :api/get-writers-for-objective
                                :api/get-objectives-for-writer :api/get-draft :api/get-drafts-for-objective
                                :api/get-sections :api/get-section :api/get-annotations :api/get-admin-removals})))

(defn common-middleware [h]
  (-> h
      wrap-keyword-params
      wrap-params
      wrap-json-params
      wrap-json-response))

(defn front-end-middleware [h config]
  (-> h
      (wrap-tower (:translation config))
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only true}
                     :store        (:session-store config)})
      (wrap-xss-protection true {:mode :block})
      (wrap-frame-options :sameorigin)))

(defn profiling-middleware [h config]
  (if-let [profile-middleware (:profile-middleware config)]
    (do
      (prn "adding profiling middleware")
      (profile-middleware h))
    h))

(defn front-end-handler [config]
  (let [handler-map (front-end-handlers)]
    (-> (make-handler routes/front-end-routes (some-fn handler-map #(when (fn? %) %)))
        (m/wrap-not-found front-end-handlers/error-404)
        (friend/authenticate (:authentication config))
        (profiling-middleware config)
        (front-end-middleware config)
        (common-middleware))))

(defn back-end-handler []
  (let [handler-map (back-end-handlers)]
    (-> (make-handler routes/back-end-routes (some-fn handler-map #(when (fn? %) %)))
        (common-middleware))))

(defn get-bearer-token-details []
  (-> (:api-credentials config/environment)
      (utils/select-all-or-nothing [:bearer-name :bearer-token])))

(defn store-admin [auth-provider-user-id]
  (when-not (users/get-admin-by-auth-provider-user-id auth-provider-user-id)
    (log/info "Storing an admin")
    (users/store-admin! {:auth-provider-user-id auth-provider-user-id})))

(defn initialise-api []
  (when-let [bearer-token-details (get-bearer-token-details)]
    (if (bt/get-token (bearer-token-details :bearer-name))
      (bt/update-token! bearer-token-details)
      (do
        (log/info "Storing bearer token details")
        (bt/store-token! bearer-token-details))))
  (when-let [admins-var (:admins config/environment)]
    (let [admins (clojure.string/split admins-var #" ")]
      (doall (map store-admin admins)))))

(def app-config
  {:authentication {:allow-anon? true
                    :workflows   (if (all-okta-credentials-provided?)
                                   [(okta-workflow (:okta-credentials config/environment))
                                    sign-up-workflow]
                                   [(if (= (:fake-twitter-mode config/environment) "TRUE")
                                      stub-twitter-workflow
                                      (twitter-workflow (configure-twitter (:twitter-credentials config/environment))))
                                    (stonecutter/workflow (soc/configure (:stonecutter-auth-provider-url config/environment)
                                                                         (:stonecutter-client-id config/environment)
                                                                         (:stonecutter-client-secret config/environment)
                                                                         (str "https://" (:base-uri config/environment)
                                                                              "/d-cent-callback")
                                                                         :protocol :openid))
                                    (facebook-workflow (:facebook-credentials config/environment))
                                    sign-up-workflow])
                    :login-uri   (if (all-okta-credentials-provided?)
                                   "/okta-sign-in"
                                   "/sign-in")}
   :session-store  (memory-store)
   :translation    (configure-translations)
   :db-spec        (db/spec (:db-config config/environment))})

(def front-end-server (atom nil))
(def back-end-server (atom nil))

(defn start-server
  ([]
   (start-server app-config))

  ([config]
   (let [front-end-port (:front-end-port config/environment)
         api-port (:api-port config/environment)]
     (db/connect! (:db-spec config))
     (initialise-api)
     (log/info (str "Starting objective8 front-end on port " front-end-port))
     (reset! front-end-server (run-server (front-end-handler config)
                                          {:port front-end-port}))
     (log/info (str "Starting objective8 api on port " api-port))
     (reset! back-end-server (run-server (back-end-handler) {:port api-port})))))

(defn -main []
  (start-server))

(defn stop-back-end-server []
  (when-not (nil? @back-end-server)
    (log/info "Stopping objective8 api")
    (@back-end-server)
    (reset! back-end-server nil)))

(defn stop-front-end-server []
  (when-not (nil? @front-end-server)
    (log/info "Stopping objective8 front-end")
    (@front-end-server)
    (reset! front-end-server nil)))

(defn restart-server []
  (stop-front-end-server)
  (stop-back-end-server)
  (start-server))
