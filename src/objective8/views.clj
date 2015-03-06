(ns objective8.views
  (:require [cemerick.friend :as friend]
            [objective8.responses :as responses]))

(defn make-view-context [page-name request]
  (let [auth-map (friend/current-authentication request)
        translations (:t' request)]
    {:translations translations 
     :ring-request request
     :user (when auth-map {:display-name (:display-name auth-map)})
     :doc (when (and page-name translations) {:title (translations (keyword (str page-name "/doc-title")))
                                              :description (translations (keyword (str page-name "/doc-description")))})}))

(defn view
  "Wraps a template so that it can easily be called
  from a handler by converting ring-requests to view-contexts"
  [viewfn]
  (fn [page-name ring-request] 
    (viewfn (make-view-context page-name ring-request))))

(def objective-detail-page
  (view responses/index-page))

