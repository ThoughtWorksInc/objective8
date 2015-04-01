(ns objective8.templates.sign-up 
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as f]
            [objective8.templates.template-functions :as tf]))

(def sign-up-template (html/html-resource "templates/jade/sign-up.html" {:parser jsoup/parser}))

(defn sign-up-page [{:keys [translations data doc] :as context}]
  (let [objective (:objective data)]
    (apply str
           (html/emit*
             (tf/translate context
                           (f/add-google-analytics
                             (html/at sign-up-template 
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                                      [:.clj-status-bar] (html/substitute (f/status-flash-bar context))

                                      [:.clj-sign-up-form] (html/prepend (html/html-snippet (anti-forgery-field)))
                                      [:.clj-username-error] (when-let [error-type (get-in doc [:errors :username])]
                                                               (html/content (translations (keyword "sign-up" (name error-type))))))))))))
