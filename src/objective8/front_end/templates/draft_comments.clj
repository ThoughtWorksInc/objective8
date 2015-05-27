(ns objective8.front-end.templates.draft-comments
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [cemerick.url :as url]
            [objective8.front-end.config :as fe-config]
            [objective8.utils :as utils]
            [objective8.front-end.api.domain :as domain]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))

(def draft-comments-template (html/html-resource "templates/jade/draft-comments.html" {:parser jsoup/parser}))

(def navigation-snippet (html/select draft-comments-template [:.clj-secondary-navigation]))

(defn draft-comments-navigation [{:keys [data translations] :as context}]
  (let  [offset (:offset data)
         {draft-id :_id :as draft} (:draft data)
         {objective-id :_id :as objective} (:objective data)
         total-comments (get-in draft [:meta :comments-count])
         comments-url (url/url (utils/path-for :fe/get-comments-for-draft :id objective-id :d-id draft-id)) 
         ]
    (html/at navigation-snippet 
             [:.clj-parent-link] (html/set-attr :href (utils/path-for :fe/draft :id objective-id :d-id draft-id))
             [:.clj-parent-text] (html/content (str (translations :draft-comments/draft-prefix) " : " 
                                                          (utils/iso-time-string->pretty-time (:_created_at draft))))
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

(defn draft-comments-page [{:keys [data doc] :as context}]
  (let [{objective-id :_id :as objective} (:objective data)]
    (->> (html/at draft-comments-template
                  [:title] (html/content (:title doc))
                  [:.clj-secondary-navigation] (html/substitute (draft-comments-navigation context)) 
                  
                  [:.clj-comment-list] (html/content (pf/comment-list context)))
         pf/add-google-analytics
         (tf/translate context)
         html/emit*
         (apply str))))
