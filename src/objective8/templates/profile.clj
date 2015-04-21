(ns objective8.templates.profile
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.permissions :as permissions]
            [objective8.utils :as utils]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))

(def profile-template (html/html-resource "templates/jade/profile.html" {:parser jsoup/parser}))

(defn profile-page [{:keys [doc data translations user] :as context}]
  (let [user-profile (:user-profile data)
        profile-owner (:profile-owner data)
        joined-date (:joined-date data)
        current-user (:username user)]
  (apply str
         (html/emit*
           (tf/translate context
                         (pf/add-google-analytics
                           (html/at profile-template
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                    [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                                    [:.clj-writer-profile] (if user-profile 
                                                             identity
                                                             (html/content (translations :profile/no-profile-message)))
                                    [:.clj-edit-profile-button] (when (and (= current-user profile-owner) 
                                                                           (permissions/writer? user)) 
                                                                  (html/set-attr :href (utils/path-for :fe/edit-profile-get)))
                                    [:.clj-writer-name] (html/content (:name user-profile))
                                    [:.clj-writer-joined-date] (html/content joined-date)
                                    [:.clj-writer-biog] (html/content (:biog user-profile))
                                    [:.clj-writer-links] nil)))))))
