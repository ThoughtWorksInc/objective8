(ns objective8.templates.draft
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.templates.page-furniture :as f]
            [objective8.utils :as utils]))

(def library-html "templates/jade/library.html")
(def draft-list-template (html/html-resource "templates/jade/draft-list.html" {:parser jsoup/parser}))
(def draft-template (html/html-resource "templates/jade/draft.html" {:parser jsoup/parser}))

(html/defsnippet no-drafts
  library-html [:.clj-no-drafts-yet] [translations]
  [:.clj-no-drafts-yet-text] (html/content (translations :draft-list/no-drafts)))

(defn- local-draft-path
  ([draft] (local-draft-path draft false))
  ([draft latest] (if draft (utils/local-path-for :fe/draft :id (:objective-id draft) :d-id
                                       (if latest "latest" (:_id draft))))))

(defn previous-drafts [previous-drafts]
  (html/transformation [:.clj-previous-draft-item]
                       (html/clone-for [draft previous-drafts]
                                       [:.clj-previous-draft-link] (html/set-attr "href" (local-draft-path draft))
                                       [:.clj-previous-draft-writer] (html/content (:username draft))
                                       [:.clj-previous-draft-time] (html/content (utils/iso-time-string->pretty-time (:_created_at draft))))))

(defn latest-draft [drafts translations]
  (let [draft (first drafts)]
    (html/transformation [:.clj-latest-draft-title] (html/content (translations :draft-list/latest-draft))
                         [:.clj-latest-draft-link] (html/set-attr "href" (local-draft-path draft true))
                         [:.clj-latest-draft-writer] (html/content (:username draft))
                         [:.clj-latest-draft-time] (html/content (utils/iso-time-string->pretty-time (:_created_at draft))))))

(defn drafts-list [{:keys [translations data user] :as context}]
  (let [drafts (:drafts data)
        objective (:objective data)]
    (html/transformation
      [:.clj-latest-draft-wrapper] (if (empty? drafts)
                                     (html/substitute (no-drafts translations))
                                     (latest-draft drafts translations))

      [:.clj-add-a-draft] (when (utils/writer-for? user (:_id objective))
                            (html/do->
                              (html/set-attr :href
                                             (utils/local-path-for :fe/add-draft-get
                                                                   :id (:_id objective)))
                              (html/content (translations :draft-list/add-a-draft))))

      [:.clj-previous-drafts-title] (html/content (translations :draft-list/previous-versions))

      [:.clj-previous-drafts-list] (if (empty? (rest drafts))
                                     (html/substitute (translations :draft-list/no-previous-versions))
                                     (previous-drafts (rest drafts)))

      [:.clj-writers-section-title] (html/content (translations :draft-list/writers))
      [:.clj-writer-item-list] (html/content (f/writer-list context)))))

(defn draft-list-page [{:keys [translations data] :as context}]
  (let [objective (:objective data)]
    (apply str
           (html/emit*
             (html/at draft-list-template
                      [:title] (html/content (get-in context [:doc :title]))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-status-bar] (html/substitute (f/status-flash-bar context))
                      [:.clj-objective-progress-indicator] nil
                      [:.clj-guidance-buttons] nil

                      [:.clj-guidance-heading] (html/content (translations :draft-guidance/heading))
                      [:.clj-guidance-text-line-1] (html/content (translations :draft-guidance/text-line-1))
                      [:.clj-guidance-text-line-2] (html/content (translations :draft-guidance/text-line-2))
                      [:.clj-guidance-text-line-3] (html/content (translations :draft-guidance/text-line-3))

                      [:.clj-draft-list-title] (html/content (str (translations :draft-list/drafts-for) ": "
                                                                  (:title objective)))

                      [:.clj-drafts-wrapper] (if (:drafting-started objective)
                                               (drafts-list context)
                                               (html/substitute (str (translations :draft-list/drafting-begins)
                                                                     " " (:drafting-begins-in objective)))))))))

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
        {objective-id :_id} (:objective data)]
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
      [:.clj-writer-item-list] (html/content (f/writer-list context)) 

      [:.clj-draft-comments] nil)))

(defn draft-page [{:keys [translations data] :as context}]
  (let [objective (:objective data)]
    (apply str
           (html/emit*
             (html/at draft-template
                      [:title] (html/content (get-in context [:doc :title]))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-status-bar] (html/substitute (f/status-flash-bar context))

                      [:.clj-guidance-buttons] nil
                      [:.clj-guidance-heading] (html/content (translations :draft-guidance/heading))
                      [:.clj-guidance-text-line-1] (html/content (translations :draft-guidance/text-line-1))
                      [:.clj-guidance-text-line-2] (html/content (translations :draft-guidance/text-line-2))
                      [:.clj-guidance-text-line-3] (html/content (translations :draft-guidance/text-line-3))

                      [:.clj-draft-wrapper] (if (:drafting-started objective)
                                              (draft-wrapper context)
                                              (html/substitute (str (translations :draft/drafting-begins)
                                                                    " " (:drafting-begins-in objective)))))))))
