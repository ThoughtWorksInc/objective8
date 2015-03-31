(ns objective8.views
  (:require [cemerick.friend :as friend]
            [objective8.responses :as responses]
            [objective8.templates.index :as index]
            [objective8.templates.learn-more :as learn-more]
            [objective8.templates.create-objective :as create-objective]
            [objective8.templates.objective :as objective]
            [objective8.templates.objective-list :as objective-list]
            [objective8.templates.question :as question]
            [objective8.templates.add-question :as add-question]
            [objective8.templates.invite-writer :as invite-writer]
            [objective8.templates.draft :as draft]
            [objective8.templates.draft-list :as draft-list]
            [objective8.templates.add-draft :as add-draft]
            [objective8.templates.sign-in :as sign-in]  
            [objective8.templates.sign-up :as sign-up]))


(defn- user-info [request auth-map]
  (when auth-map {:username (:username auth-map)
                  :roles (:roles auth-map)}))

(defn- doc-info [request page-name translations data]
  (when (and page-name translations) 
    (-> {:flash (:flash request)}
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

(defn- render-page [page]
  (fn [context] (responses/rendered-response page context)))

(def project-status (view (render-page responses/project-status-page)))

(def four-o-four (view (render-page responses/error-404-page)))

(def index (view index/index-page))
(def objective-list (view objective-list/objective-list-page))
(def learn-more-page (view learn-more/learn-more-page))
(def create-objective (view create-objective/create-objective-page))
(def objective-detail-page (view objective/objective-page))
(def question-page (view question/question-page))
(def add-question-page (view add-question/add-question-page))
(def invite-writer-page (view invite-writer/invite-writer-page))
(def draft-list (view draft-list/draft-list-page))
(def draft (view draft/draft-page))
(def add-draft (view add-draft/add-draft-page))
(def sign-in (view sign-in/sign-in-page))
(def sign-up (view sign-up/sign-up-page))
