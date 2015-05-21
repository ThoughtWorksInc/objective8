(ns objective8.front-end.views
  (:require [cemerick.friend :as friend]
            [objective8.front-end.templates.index :as index]
            [objective8.front-end.templates.learn-more :as learn-more]
            [objective8.front-end.templates.project-status :as project-status]
            [objective8.front-end.templates.create-objective :as create-objective]
            [objective8.front-end.templates.objective :as objective]
            [objective8.front-end.templates.objective-comments :as objective-comments]
            [objective8.front-end.templates.objective-list :as objective-list]
            [objective8.front-end.templates.question :as question]
            [objective8.front-end.templates.add-question :as add-question]
            [objective8.front-end.templates.invite-writer :as invite-writer]
            [objective8.front-end.templates.create-profile :as create-profile]
            [objective8.front-end.templates.edit-profile :as edit-profile]
            [objective8.front-end.templates.profile :as profile]
            [objective8.front-end.templates.draft :as draft]
            [objective8.front-end.templates.draft-comments :as draft-comments]
            [objective8.front-end.templates.draft-list :as draft-list]
            [objective8.front-end.templates.draft-section :as draft-section]
            [objective8.front-end.templates.draft-diff :as draft-diff]
            [objective8.front-end.templates.add-draft :as add-draft]
            [objective8.front-end.templates.import-draft :as import-draft]
            [objective8.front-end.templates.dashboard-questions :as dashboard-questions]
            [objective8.front-end.templates.dashboard-comments :as dashboard-comments]
            [objective8.front-end.templates.dashboard-annotations :as dashboard-annotations]
            [objective8.front-end.templates.sign-in :as sign-in]  
            [objective8.front-end.templates.sign-up :as sign-up]
            [objective8.front-end.templates.admin-activity :as admin-activity]
            [objective8.front-end.templates.admin-removal-confirmation :as admin-removal-confirmation]
            [objective8.front-end.templates.error-404 :as error-404]
            [objective8.front-end.templates.error-configuration :as error-configuration]))


(defn- user-info [request auth-map]
  (when auth-map {:username (:username auth-map)
                  :roles (:roles auth-map)}))

(defn- doc-info [request page-name translations data]
  (when (and page-name translations) 
    (-> {:flash (:flash request)}
        (assoc :page-name page-name)
        (assoc :errors (:errors data))
        (assoc :title
               (if-let [title (get-in data [:doc :title])]
                 title
                 (translations (keyword (str page-name "/doc-title")))))
        (assoc :description
               (if-let [description (get-in data [:doc :description])]
                 description
                 (translations (keyword (str page-name "/doc-description"))))))))

(defn make-view-context [page-name request data]
  (let [auth-map (friend/current-authentication request)
        translations (:t' request)
        data (apply hash-map data)]
    {:translations translations 
     :ring-request request
     :user (user-info request auth-map)
     :doc (doc-info request page-name translations data)
     :invitation-rsvp (get-in request [:session :invitation])
     :data data}))

(defn view
  "Wraps a template so that it can easily be called
  from a handler by converting ring-requests to view-contexts"
  [viewfn]
  (fn [page-name ring-request & data] 
    (viewfn (make-view-context page-name ring-request data))))

(def error-404 (view error-404/error-404-page))
(def error-configuration (view error-configuration/error-configuration-page))

(def index (view index/index-page))
(def learn-more-page (view learn-more/learn-more-page))
(def project-status (view project-status/project-status-page))
(def objective-list (view objective-list/objective-list-page))
(def create-objective (view create-objective/create-objective-page))
(def objective-detail-page (view objective/objective-page))
(def objective-comments-view (view objective-comments/objective-comments-page))
(def question-page (view question/question-page))
(def add-question-page (view add-question/add-question-page))
(def invite-writer-page (view invite-writer/invite-writer-page))
(def create-profile (view create-profile/create-profile-page))
(def edit-profile (view edit-profile/edit-profile-page))
(def profile (view profile/profile-page))
(def draft-list (view draft-list/draft-list-page))
(def draft (view draft/draft-page))
(def draft-comments-view (view draft-comments/draft-comments-page))
(def draft-section (view draft-section/draft-section-page))
(def draft-diff (view draft-diff/draft-diff-page))
(def add-draft (view add-draft/add-draft-page))
(def import-draft (view import-draft/import-draft-page))
(def dashboard-questions-page (view dashboard-questions/dashboard-questions))
(def dashboard-comments-page (view dashboard-comments/dashboard-comments))
(def dashboard-annotations-page (view dashboard-annotations/dashboard-annotations))
(def sign-in (view sign-in/sign-in-page))
(def sign-up (view sign-up/sign-up-page))
(def admin-activity (view admin-activity/admin-activity-page))
(def admin-removal-confirmation (view admin-removal-confirmation/admin-removal-confirmation-page))

