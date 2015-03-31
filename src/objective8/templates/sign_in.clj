(ns objective8.templates.sign-in 
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as f]))

(def sign-in-template (html/html-resource "templates/jade/sign-in.html" {:parser jsoup/parser}))

(defn sign-in-page [{:keys [translations data doc] :as context}]
  (let [objective (:objective data)]
    (apply str
           (html/emit*
             (f/add-google-analytics
               (html/at sign-in-template 
                        [:title] (html/content (:title doc))
                        [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                        [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                        [:.clj-status-bar] (html/substitute (f/status-flash-bar context))

                        [:.l8n-sign-in-title] (html/content (translations :sign-in/page-title))
                        [:.l8n-sign-in-with-twitter] (html/content (translations :sign-in/sign-in-with-twitter))
                        [:.l8n-twitter-help-text] (html/content (translations :sign-in/twitter-help-text))))))))
