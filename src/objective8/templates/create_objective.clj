(ns objective8.templates.create-objective 
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))

(def create-objective-template (html/html-resource "templates/jade/create-objective.html" {:parser jsoup/parser}))

(defn create-objective-page [{:keys [doc] :as context}]
  (apply str
         (html/emit*
           (tf/translate context
                         (pf/add-google-analytics
                           (html/at create-objective-template
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                    [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

                                    [:.clj-guidance-buttons] nil
                                    [:.clj-create-objective-form] (html/prepend (html/html-snippet (anti-forgery-field)))))))))
