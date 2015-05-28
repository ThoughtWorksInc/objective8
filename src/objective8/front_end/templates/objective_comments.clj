(ns objective8.front-end.templates.objective-comments
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [cemerick.url :as url]
            [objective8.front-end.config :as fe-config]
            [objective8.utils :as utils]
            [objective8.front-end.api.domain :as domain]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))

(def objective-comments-template (html/html-resource "templates/jade/objective-comments.html" {:parser jsoup/parser}))

(def objective-comments-navigation-snippet (html/select objective-comments-template [:.clj-secondary-navigation]))

(defn objective-comments-navigation [{:keys [data] :as context}]
  (let [offset (:offset data) 
        objective (:objective data)
        objective-id (:_id objective)
        total-comments (get-in objective [:meta :comments-count])
        comments-url (url/url (utils/path-for :fe/get-comments-for-objective :id objective-id))]
    (html/at objective-comments-navigation-snippet
             [:.clj-parent-link] (html/set-attr :href (-> (utils/path-for :fe/objective :id objective-id)
                                                          url/url
                                                          (assoc :anchor "comments")))
             [:.clj-parent-text] (html/content (:title objective))
             [:.clj-secondary-navigation-previous] 
             (when (> offset 0) 
               (html/transformation
                 [:.clj-secondary-navigation-previous-link] 
                 (html/set-attr :href
                                (-> comments-url
                                    (assoc :query {:offset (max 0 (- offset fe-config/comments-pagination))})
                                    str))))
             [:.clj-secondary-navigation-next] 
             (when (> total-comments (+ offset fe-config/comments-pagination))
               (html/transformation
                 [:.clj-secondary-navigation-next-link] 
                 (html/set-attr :href
                                (-> comments-url
                                    (assoc :query {:offset (+ offset fe-config/comments-pagination)})
                                    str)))))))

(defn objective-comments-page [{:keys [doc] :as context}]
  (->> (html/at objective-comments-template
                [:title] (html/content (:title doc))

                [:.clj-secondary-navigation] (html/substitute (objective-comments-navigation context))

                [:.clj-comment-list] (html/content (pf/comment-list context)))
       pf/add-google-analytics
       (tf/translate context)
       html/emit*
       (apply str)))
