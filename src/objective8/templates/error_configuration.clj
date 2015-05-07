(ns objective8.templates.error-configuration
  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :as f]
            [objective8.templates.template-functions :as tf])) 

(def error-configuration-template (html/html-resource "templates/jade/error-configuration.html"))

(defn error-configuration-page [{:keys [doc] :as context}]
  (apply str
         (html/emit*
           (tf/translate context
                         (f/add-google-analytics
                           (html/at error-configuration-template
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                                    [:.clj-status-bar] (html/substitute (f/status-flash-bar context))))))))
