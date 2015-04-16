(ns objective8.templates.create-profile
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))

(def create-profile-template (html/html-resource "templates/jade/create-profile.html" {:parser jsoup/parser}))

(defn create-profile-page [{:keys [doc] :as context}]
  (apply str
         (html/emit*
           (tf/translate context
                         (pf/add-google-analytics
                           (html/at create-profile-template
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                    [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))))))))
