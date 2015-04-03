(ns objective8.templates.draft
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.templates.template-functions :as tf]
            [objective8.templates.page-furniture :as pf]
            [objective8.utils :as utils]))

(def draft-template (html/html-resource "templates/jade/draft.html" {:parser jsoup/parser}))

(defn previous-draft-navigation [{:keys [data translations] :as context}]
  (let [draft (:draft data)]
    (html/transformation
      [:.clj-draft-version-previous-link] (html/set-attr "href" 
                                                         (utils/local-path-for :fe/draft :id (:objective-id draft) 
                                                                               :d-id (:previous-draft-id draft)))
      [:.clj-draft-version-navigation-previous-text] (html/content (translations :draft/previous-draft)))))

(defn next-draft-navigation [{:keys [data translations] :as context}]
  (let [draft (:draft data)]
    (html/transformation
      [:.clj-draft-version-next-link] (html/set-attr "href" 
                                                     (utils/local-path-for :fe/draft :id (:objective-id draft) 
                                                                           :d-id (:next-draft-id draft)))
      [:.clj-draft-version-navigation-next-text] (html/content (translations :draft/next-draft)))))

(defn draft-version-navigation [{:keys [data] :as context}]
  (let [draft (:draft data)]
    (html/transformation
      [:.clj-draft-version-writer-author] (html/content (:username draft))
      [:.clj-draft-version-time] (html/content (utils/iso-time-string->pretty-time (:_created_at draft)))
      [:.clj-draft-version-navigation-previous] (when (:previous-draft-id draft)
                                                  (previous-draft-navigation context))
      [:.clj-draft-version-navigation-next] (when (:next-draft-id draft)
                                              (next-draft-navigation context)))))


(defn draft-wrapper [{:keys [data translations user] :as context}]
  (let [draft (:draft data)
        objective (:objective data)
        {objective-id :_id objective-status :status} objective
        optionally-disable-voting (if (tf/in-drafting? objective) 
                                    identity
                                    pf/disable-voting-actions)]

    (html/transformation
      [:.clj-draft-version-navigation] (if draft
                                         (draft-version-navigation context)
                                         (html/content (translations :draft/no-draft)))
      [:.clj-add-a-draft] (when (utils/writer-for? user objective-id)
                            (html/do->
                              (html/set-attr "href"
                                             (utils/local-path-for :fe/add-draft-get
                                                                   :id objective-id))
                              (html/content (translations :draft/add-a-draft))))

      [:.clj-draft-preview-document] (when-let [draft-content (:draft-content data)] 
                                       (html/html-content draft-content)) 

      [:.clj-writers-section-title] (html/content (translations :draft/writers))
      [:.clj-writer-item-list] (html/content (pf/writer-list context))
      [:.clj-draft-comments] (when draft
                               (html/transformation
                                [:.l8n-comments-section-title] (html/content (translations :draft/comments))
                                [:.clj-comment-list] (html/content (optionally-disable-voting (pf/comment-list context)))
                                [:.clj-comment-create] (html/content (pf/comment-create context :draft)))))))

(def drafting-begins-in-snippet (html/select pf/library-html-resource [:.clj-drafting-begins-in]))

(defn drafting-begins-in [{:keys [data] :as context}]
  (let [end-date (get-in data [:objective :end-date])
        drafting-begins-in-days (get-in data [:objective :days-until-drafting-begins])]
    (html/at drafting-begins-in-snippet
             [:.clj-drafting-begins-in] (html/set-attr :drafting-begins-date end-date)
             [:.clj-drafting-begins-in-days] (html/content (str drafting-begins-in-days)))))

(defn draft-page [{:keys [translations data doc] :as context}]
  (let [objective (:objective data)]
    (apply str
           (html/emit*
             (pf/add-google-analytics
               (html/at draft-template
                        [:title] (html/content (:title doc))
                        [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                        [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                        [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

                        [:.clj-guidance-buttons] nil
                        [:.clj-guidance-heading] (html/content (translations :draft-guidance/heading))

                        [:.clj-draft-wrapper] (if (tf/in-drafting? objective)
                                                (draft-wrapper context)
                                                (html/content (drafting-begins-in context)))))))))
