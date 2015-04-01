(ns objective8.templates.project-status 
  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.template-functions :as tf]   
            [objective8.templates.page-furniture :as f])) 

(def project-status-template (html/html-resource "templates/jade/project-status.html"))

(defn project-status-page [{:keys [translations doc] :as context}]
  (apply str
         (html/emit*
           (tf/translate context
                         (f/add-google-analytics
                           (html/at project-status-template
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                                    [:.clj-status-bar] (html/substitute (f/status-flash-bar context))))))))
