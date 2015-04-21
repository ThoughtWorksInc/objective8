(ns objective8.templates.edit-profile
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))

(def edit-profile-template (html/html-resource "templates/jade/edit-profile.html" {:parser jsoup/parser}))

(defn edit-profile-page [{:keys [doc data] :as context}]
  (let [user-profile (:user-profile data)]
    (apply str
           (html/emit*
             (tf/translate context
                           (pf/add-google-analytics
                             (html/at edit-profile-template
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                      [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                      [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                                      [:.clj-create-profile-form] (html/prepend (html/html-snippet (anti-forgery-field)))
                                      [:.clj-edit-profile-name] (html/content (:name user-profile))
                                      [:.clj-edit-profile-biog] (html/content (:biog user-profile)))))))))
