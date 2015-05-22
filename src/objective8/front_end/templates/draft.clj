(ns objective8.front-end.templates.draft
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.config :as fe-config]
            [objective8.front-end.templates.template-functions :as tf]
            [objective8.front-end.api.domain :as domain]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.utils :as utils]
            [objective8.front-end.permissions :as permissions]))

(def draft-template (html/html-resource "templates/jade/draft.html" {:parser jsoup/parser}))

(def draft-version-navigation-snippet (html/select draft-template [:.clj-draft-version-navigation]))

(defn draft-version-navigation [{:keys [data] :as context}]
  (let [draft (:draft data)
        objective-id (:objective-id draft)
        previous-id (:previous-draft-id draft)
        next-id (:next-draft-id draft)] 
    (html/at draft-version-navigation-snippet
             [:.clj-all-drafts-link] (html/set-attr :href (utils/path-for :fe/draft-list :id objective-id))
             [:.clj-draft-version-navigation-previous] 
             (when previous-id
               (html/transformation
                 [:.clj-draft-version-previous-link] 
                 (html/set-attr :href
                                (utils/local-path-for :fe/draft :id objective-id
                                                      :d-id previous-id))))
             [:.clj-draft-version-navigation-next] 
             (when next-id
               (html/transformation
                 [:.clj-draft-version-next-link] 
                 (html/set-attr :href
                                (utils/local-path-for :fe/draft :id objective-id
                                                      :d-id next-id)))))))

(def section-link-snippet (first (html/select draft-template [:.draft-add-inline-comment])))

(defn update-section-link-with-label [label {:keys [data] :as context}]
  (let [draft-id  (get-in data [:draft :_id]) 
        objective-id (get-in data [:draft :objective-id])]
    (html/at section-link-snippet
             [:.clj-annotation-link] (html/set-attr :href (utils/path-for :fe/draft-section :id objective-id :d-id draft-id :section-label label)))))

(defn add-section-link [context node]
  (let [section-label (get-in node [:attrs :data-section-label])]
    (->  (update-section-link-with-label section-label context) 
    (list node))))

(defn add-section-links [draft-content context]
  (let [draft-resource (-> (.getBytes draft-content "UTF-8")
                           java.io.ByteArrayInputStream. 
                           (html/html-resource {:parser jsoup/parser})
                           (html/select  [:body]) 
                           first
                           :content)]
    (html/at draft-resource
             [(html/attr? :data-section-label)] (partial add-section-link context))))

(def no-drafts-snippet (html/select pf/library-html-resource [:.clj-no-drafts-yet]))

(def draft-wrapper-snippet (html/select draft-template [:.clj-draft-wrapper]))

(def comment-history-snippet (html/select draft-template [:.clj-comment-history-item]))

(defn draft-wrapper [{:keys [data user] :as context}]
  (let [{draft-id :_id :as draft} (:draft data)
        objective (:objective data)
        {objective-id :_id objective-status :status} objective
        optionally-disable-voting (if (domain/in-drafting? objective) 
                                    identity
                                    pf/disable-voting-actions)
        comments (:comments data)
        number-of-comments-shown (count comments)
        comment-history-link (when draft-id (str (utils/path-for :fe/get-comments-for-draft
                                                                 :id objective-id
                                                                 :d-id draft-id)
                                                 "?offset=" fe-config/comments-pagination))]
    (html/at draft-wrapper-snippet
             [:.clj-draft-version-navigation] (if draft
                                                (html/substitute (draft-version-navigation context))
                                                (html/content no-drafts-snippet))
             [:.clj-draft-version-writer-author] (when draft (html/content (:username draft)))

             [:.clj-draft-version-time] (when draft (html/content (utils/iso-time-string->pretty-time (:_created_at draft)))) 

             [:.clj-draft-preview-document] (when-let [draft-content (:draft-content data)] 
                                              (html/content (add-section-links draft-content context)))

             [:.clj-what-changed-link] (when (:previous-draft-id draft)
                                         (html/set-attr :href 
                                                        (utils/path-for :fe/draft-diff
                                                                        :id objective-id
                                                                        :d-id (:_id draft))))

             [:.clj-writer-item-list] (html/content (pf/writer-list context))
             [:.clj-draft-comments] (when draft
                                      (html/transformation
                                        [:.clj-comment-list] (html/content (optionally-disable-voting (pf/comment-list context)))
                                        [:.clj-comment-list] (if (< number-of-comments-shown (get-in draft [:meta :comments-count]))
                                                               (html/append comment-history-snippet)
                                                               identity)

                                        [:.clj-comment-history-link] (html/set-attr :href comment-history-link)
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
             (tf/translate context
                           (pf/add-google-analytics
                             (html/at draft-template
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                      [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                      [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

                                      [:.clj-guidance-buttons] nil
                                      [:.clj-guidance-heading] (html/content (translations :draft-guidance/heading))

                                      [:.clj-draft-wrapper] (if (domain/in-drafting? objective)
                                                              (html/substitute (draft-wrapper context))
                                                              (html/content (drafting-begins-in context))))))))))
