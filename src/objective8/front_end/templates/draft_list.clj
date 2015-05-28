(ns objective8.front-end.templates.draft-list
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [cemerick.url :as url]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.front-end.templates.template-functions :as tf]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.permissions :as permissions]))

(def library-html "templates/jade/library.html")
(def draft-list-template (html/html-resource "templates/jade/draft-list.html" {:parser jsoup/parser}))
(def latest-draft-wrapper-snippet (html/select draft-list-template [:.clj-latest-draft]))
(def drafts-wrapper-snippet (html/select draft-list-template [:.clj-drafts-wrapper]))
(def previous-draft-item-snippet (html/select draft-list-template [:.clj-previous-draft-item])) 

(def no-drafts-snippet (html/select pf/library-html-resource [:.clj-no-drafts-yet]))


(defn- local-draft-path
  ([draft] (local-draft-path draft false))
  ([draft latest] (if draft (utils/local-path-for :fe/draft :id (:objective-id draft) :d-id
                                                  (if latest "latest" (:_id draft))))))

(defn previous-drafts [previous-drafts]
  (html/at previous-draft-item-snippet
           [:.clj-previous-draft-item]
           (html/clone-for [draft previous-drafts]
                           [:.clj-previous-draft-link] (html/set-attr :href (local-draft-path draft))
                           [:.clj-previous-draft-writer] (html/content (:username draft))
                           [:.clj-previous-draft-time] (html/content (utils/iso-time-string->pretty-time (:_created_at draft))))))

(defn latest-draft [draft]
  (html/at latest-draft-wrapper-snippet 
           [:.clj-latest-draft-link] (html/set-attr :href (local-draft-path draft true))
           [:.clj-latest-draft-writer] (html/content (:username draft))
           [:.clj-latest-draft-time] (html/content (utils/iso-time-string->pretty-time (:_created_at draft)))))

(defn drafts-wrapper [{:keys [translations data user] :as context}]
  (let [drafts (:drafts data)
        objective (:objective data)]
    (html/at drafts-wrapper-snippet
             [:.clj-latest-draft] (if (empty? drafts)
                                            (html/substitute no-drafts-snippet)
                                            (html/substitute (latest-draft (first drafts))))

             [:.clj-add-a-draft] (when (permissions/writer-for? user (:_id objective))
                                   (html/set-attr :href
                                                  (utils/local-path-for :fe/add-draft-get
                                                                        :id (:_id objective))))

             [:.clj-import-draft-link] (when (permissions/writer-for? user (:_id objective))
                                   (html/set-attr :href
                                                  (utils/local-path-for :fe/import-draft-get
                                                                        :id (:_id objective)))))))

(defn draft-list-page [{:keys [translations data doc] :as context}]
  (let [objective (:objective data)
        drafts (:drafts data)]
    (apply str
           (html/emit*
             (tf/translate context
                           (pf/add-google-analytics
                             (html/at draft-list-template
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                      [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                      [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                                      [:.clj-objective-progress-indicator] (html/substitute (pf/progress-indicator context))
                                      [:.clj-progress-drafts-item] (html/add-class "on")
                                      [:.clj-progress-objective-item] (html/remove-class "on")
                                      [:.clj-guidance-buttons] nil

                                      [:.clj-guidance-heading] (html/content (translations :draft-guidance/heading))

                                      [:.clj-draft-list-title] (html/content (:title objective))

                                      [:.clj-drafts-wrapper] (html/substitute (drafts-wrapper context)) 
                                      
                                      [:.clj-previous-drafts-list] (if (empty? (rest drafts))
                                                                     (html/substitute (translations :draft-list/no-previous-versions))
                                                                     (html/content (previous-drafts (rest drafts))))

                                      [:.clj-writer-item-list] (html/content (pf/writer-list context))))))))) 
