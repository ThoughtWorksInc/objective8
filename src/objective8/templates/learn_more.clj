(ns objective8.templates.learn-more
  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf])) 

(def learn-more (html/html-resource "templates/jade/learn-more.html"))

(defn learn-more-page [{:keys [translations doc] :as context}]
  (apply str
         (html/emit*
           (tf/translate context
                         (pf/add-google-analytics
                           (html/at learn-more
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                    [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))))))))
