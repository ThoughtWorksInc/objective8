(ns objective8.front-end.templates.sign-in 
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.templates.page-furniture :as f]
            [objective8.front-end.templates.template-functions :as tf]))

(def sign-in-template (html/html-resource "templates/jade/sign-in.html" {:parser jsoup/parser}))

(defn sign-in-page [{:keys [data doc] :as context}]
  (apply str
         (html/emit*
           (tf/translate context
                         (f/add-google-analytics
                           (html/at sign-in-template 
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                                    [:.clj-status-bar] (html/substitute (f/status-flash-bar context))))))))
