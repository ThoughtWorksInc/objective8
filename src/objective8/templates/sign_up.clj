(ns objective8.templates.sign-up 
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as f]))

(def sign-up-template (html/html-resource "templates/jade/sign-up.html" {:parser jsoup/parser}))

(defn sign-up-page [{:keys [translations data doc] :as context}]
  (let [objective (:objective data)
        tl8 (f/translator context)]
    (apply str
           (html/emit*
             (html/at sign-up-template 
                      [:title] (html/content (:title doc))
                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-status-bar] (html/substitute (f/status-flash-bar context))

                      [:.l8n-sign-up-title] (tl8 :sign-up/page-title)
                      [:.l8n-sign-up-description] (tl8 :sign-up/welcome)

                      [:.clj-sign-up-form] (html/prepend (html/html-snippet (anti-forgery-field)))
                      [:.l8n-label-username] (tl8 :sign-up/username-label)
                      [:.clj-username-error] (if-let [error-type (get-in doc [:errors :username])]
                                               (tl8 (keyword "sign-up" (name error-type))))
                      [:.l8n-label-email-address] (tl8 :sign-up/email-label)
                      [:.l8n-sign-up] (tl8 :sign-up/button))))))
