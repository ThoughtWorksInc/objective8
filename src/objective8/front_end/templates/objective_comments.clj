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

(def enable-link
  (html/remove-class "disabled"))

(defn objective-comments-page [{:keys [data doc translations] :as context}]
  (let [objective (:objective data)
        objective-id (:_id objective)
        optionally-disable-voting (if (domain/open? objective)
                                    identity
                                    (pf/disable-voting-actions translations))
        comments (:comments data)
        offset (:offset data)
        total-comments (get-in objective [:meta :comments-count])
        comments-url (url/url (utils/path-for :fe/get-comments-for-objective :id objective-id))]
    (->> (html/at objective-comments-template
                  [:title] (html/content (:title doc))
                  [:.clj-objective-link] (html/do-> 
                                           (html/content (:title objective))
                                           (html/set-attr :href (utils/path-for :fe/objective :id objective-id)))
                  [:.clj-comments-start-index] (html/content (str (min (inc offset) total-comments)))
                  [:.clj-comments-end-index] (html/content (str (min (+ offset fe-config/comments-pagination)
                                                                     total-comments)))
                  [:.clj-comments-total-count] (html/content (str total-comments))
                  [:.clj-comments-previous] (if (> offset 0) enable-link identity)
                  [:.clj-comments-previous-link] (html/set-attr :href 
                                                                (-> comments-url
                                                                    (assoc :query {:offset (max 0 (- offset fe-config/comments-pagination))})
                                                                    str))
                  [:.clj-comments-next] (if (> total-comments (+ offset fe-config/comments-pagination)) enable-link identity)
                  [:.clj-comments-next-link] (html/set-attr :href 
                                                            (-> comments-url
                                                                (assoc :query {:offset (+ offset fe-config/comments-pagination)})
                                                                str))
                  [:.clj-comment-list] (html/content
                                        (optionally-disable-voting
                                         (pf/comment-list context))))
         pf/add-google-analytics
         (tf/translate context)
         html/emit*
         (apply str))))
