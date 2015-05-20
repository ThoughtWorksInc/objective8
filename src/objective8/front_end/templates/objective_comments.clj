(ns objective8.front-end.templates.objective-comments
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [cemerick.url :as url]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.front-end.permissions :as permissions]
            [objective8.front-end.api.domain :as domain]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))

(def objective-comments-template (html/html-resource "templates/jade/objective-comments.html" {:parser jsoup/parser}))

(def comment-pagination 50)

(defn objective-comments-page [{:keys [data doc translations] :as context}]
  (let [objective (:objective data)
        objective-id (:_id objective)
        optionally-disable-voting (if (domain/open? objective)
                                    identity
                                    (pf/disable-voting-actions translations))
        comments (:comments data)
        offset (:offset data)
        total-comments (get-in objective [:meta :comments-count])]
    (->> (html/at objective-comments-template
                  [:.clj-comments-start-index] (html/content (str (inc offset)))
                  [:.clj-comments-end-index] (html/content (str (min (+ offset comment-pagination)
                                                                     total-comments)))
                  [:.clj-comments-total-count] (html/content (str total-comments))
                  [:.clj-comment-list] (html/content
                                        (optionally-disable-voting
                                         (pf/comment-list context)))
                  [:.clj-comment-create] nil)
         pf/add-google-analytics
         (tf/translate context)
         html/emit*
         (apply str))))
