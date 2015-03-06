(ns objective8.views
  (:require [cemerick.friend :as friend]
            [objective8.responses :as responses]))

(defn- user-info [request auth-map]
  (when auth-map {:display-name (:display-name auth-map)}))

(defn- doc-info [request page-name translations]
  (when (and page-name translations) 
    {:title (translations (keyword (str page-name "/doc-title")))
     :description (translations (keyword (str page-name "/doc-description")))}))

(defn make-view-context [page-name request data]
  (let [auth-map (friend/current-authentication request)
        translations (:t' request)]
    {:translations translations 
     :ring-request request
     :user (user-info request auth-map)
     :doc (doc-info request page-name translations)
     :data (apply hash-map data)}))

(defn view
  "Wraps a template so that it can easily be called
  from a handler by converting ring-requests to view-contexts"
  [viewfn]
  (fn [page-name ring-request & data] 
    (viewfn (make-view-context page-name ring-request data))))

(def objective-detail-page
  (view responses/index-page))

