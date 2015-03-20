(ns objective8.views
  (:require [cemerick.friend :as friend]
            [objective8.responses :as responses]
            [objective8.templates.learn-more :as learn-more]
            [objective8.templates.objective :as objective]))  

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
     :invitation (get-in request [:session :invitation])
     :data data}))

(defn view
  "Wraps a template so that it can easily be called
  from a handler by converting ring-requests to view-contexts"
  [viewfn]
  (fn [page-name ring-request & data] 
    (viewfn (make-view-context page-name ring-request data))))

(defn- render-page [page]
  (fn [context] (responses/rendered-response page context)))

(def index (view (render-page responses/index-page)))
(def sign-in (view (render-page responses/sign-in-page)))
(def project-status (view (render-page responses/project-status-page)))
(def sign-up-form (view (render-page responses/sign-up)))
(def objectives-list (view (render-page responses/objective-list-page)))
(def create-objective-form (view (render-page responses/objective-create-page)))
;(def objective-detail-page (view (render-page responses/objective-detail-page)))
(def question-list (view (render-page responses/question-list-page)))
(def question-detail (view (render-page responses/question-view-page)))
(def candidate-list (view (render-page responses/candidate-list-page)))
(def invitation-response (view (render-page responses/invitation-response-page)))
(def add-draft (view (render-page responses/add-draft-page)))
(def draft-detail (view (render-page responses/draft-detail-page)))
(def draft-list (view (render-page responses/draft-list-page)))
(def drafting-not-started (view (render-page responses/drafting-not-started-page)))

(def four-o-four (view (render-page responses/error-404-page)))

(def new-learn-more-page (view learn-more/learn-more-page))
(def objective-detail-page (view objective/objective-page))
