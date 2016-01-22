(ns objective8.front-end.templates.profile
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.permissions :as permissions]
            [objective8.front-end.api.domain :as domain]
            [objective8.utils :as utils]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))

(def profile-template (html/html-resource "templates/jade/profile.html" {:parser jsoup/parser}))

(def profile-objective-list-item-resource (html/select pf/library-html-resource [:.clj-profile-objective-list-item]))

(defn- shorten-content [content]
  (let [content (or content "")
        shortened-content (clojure.string/trim (subs content 0 (min (count content) 100)))]
    (when-not (empty? shortened-content)
      (str shortened-content "..."))))

(defn brief-description [objective]
  (shorten-content (:description objective)))

(defn profile-objective-list-items [{:keys [translations data user] :as context}]
  (let [objectives (:objectives-for-writer data)]
    (html/at profile-objective-list-item-resource [:.clj-profile-objective-list-item]
             (html/clone-for [{objective-id :_id :as objective} objectives]
                             [:.clj-star-container] nil

                             [:.clj-profile-objective-list-item-removal-container] nil
                             [:.clj-profile-objective-list-dashboard-link] (when (permissions/writer-for? user objective-id)
                                                                     (html/set-attr :href (utils/path-for :fe/dashboard-questions :id objective-id))) 
                             [:.clj-profile-objective-list-item-link] (html/set-attr :href (utils/path-for :fe/objective :id objective-id))
                             [:.clj-profile-objective-list-item-title] (html/content (:title objective))

                             [:.clj-objective-brief-description]
                             (html/content (brief-description objective))))))

(defn profile-page [{:keys [doc data translations user] :as context}]
  (let [user-profile (:user-profile data)
        profile-owner (:profile-owner data)
        joined-date (:joined-date data)
        current-user (:username user)]
  (apply str
         (html/emit*
           (tf/translate context
                         (-> (html/at profile-template
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
                                    [:.clj-writer-links] nil
                                    [:.clj-profile-objective-list-item-removal-container] nil
                                    [:.clj-profile-objective-list] (html/content (profile-objective-list-items context)))
                           pf/add-google-analytics
                           pf/add-custom-favicon))))))
