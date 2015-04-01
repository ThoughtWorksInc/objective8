(ns objective8.templates.create-objective 
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as f]
            [objective8.templates.template-functions :as tf]))

(def create-objective-template (html/html-resource "templates/jade/create-objective.html" {:parser jsoup/parser}))

(defn create-objective-page [{:keys [translations data doc] :as context}]
  (let [objective (:objective data)]
    (apply str
           (html/emit*
             (tf/translate context
                           (f/add-google-analytics
                             (html/at create-objective-template
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                                      [:.clj-status-bar] (html/substitute (f/status-flash-bar context))

                                      [:.clj-guidance-buttons] nil
                                      [:.clj-create-objective-form] (html/prepend (html/html-snippet (anti-forgery-field))))))))))
