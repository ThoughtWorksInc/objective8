(ns objective8.templates.invite-writer
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))

(def invite-writer-template (html/html-resource "templates/jade/invite-writer.html" {:parser jsoup/parser}))

(def invite-writer-form-snippet (html/select pf/library-html-resource [:.clj-invite-a-writer-form]))

(defn invite-writer-form [{:keys [translations data]}]
  (html/at invite-writer-form-snippet
           [:.clj-invite-a-writer-form] (html/prepend (html/html-snippet (anti-forgery-field)))))

(def sign-in-to-invite-writer-snippet (html/select pf/library-html-resource [:.clj-to-invite-writer-please-sign-in]))

(defn sign-in-to-invite-writer [{:keys [ring-request] :as context}]
  (html/at sign-in-to-invite-writer-snippet 
           [:.clj-to-invite-writer-sign-in-link] 
           (html/set-attr :href (str "/sign-in?refer=" (:uri ring-request)))))

(defn invite-writer [{user :user :as context}]
  (if user
    (invite-writer-form context)
    (sign-in-to-invite-writer context)))

(defn invite-writer-page [{:keys [translations data doc] :as context}]
  (let [objective (:objective data)]
    (apply str
           (html/emit*
             (tf/translate context
                           (pf/add-google-analytics
                             (html/at invite-writer-template
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                      [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                      [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

                                      [:.clj-objective-navigation-item-objective] (html/set-attr :href (str "/objectives/" (:_id objective)))
                                      [:.clj-objective-title] (html/content (:title objective))

                                      [:.clj-invite-a-writer-form] (html/content (invite-writer context))
                                      [:.clj-invite-a-writer-form] (html/set-attr :action (str "/objectives/" (:_id objective) "/writer-invitations")))))))))
