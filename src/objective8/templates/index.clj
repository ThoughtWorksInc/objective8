(ns objective8.templates.index 
  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf])) 

(def index-template (html/html-resource "templates/jade/index.html"))

(defn index-page [{:keys [doc] :as context}]
  (apply str
         (html/emit*
           (tf/translate context
                         (pf/add-google-analytics 
                           (html/at index-template
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                    [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))))))))
