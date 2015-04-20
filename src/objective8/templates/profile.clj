(ns objective8.templates.profile
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))

(def profile-template (html/html-resource "templates/jade/profile.html" {:parser jsoup/parser}))

(defn profile-page [{:keys [doc data] :as context}]
  (let [user-profile (:user-profile data)]
  (apply str
         (html/emit*
           (tf/translate context
                         (pf/add-google-analytics
                           (html/at profile-template
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                    [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                                    [:.clj-writer-name] (html/content (:name user-profile))
                                    [:.clj-writer-joined-date] (html/content (:joined-date user-profile))
                                    [:.clj-writer-biog] (html/content (:biog user-profile))
                                    [:.clj-writer-links] nil)))))))

