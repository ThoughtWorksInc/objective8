(ns objective8.templates.dashboard-comments
  (:require [net.cgrand.enlive-html :as html]
            [cemerick.url :as url]
            [objective8.utils :as utils]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))


(def dashboard-comments-template (html/html-resource "templates/jade/comments-dashboard.html"))

(def dashboard-comments-comment-item-snippet (html/select dashboard-comments-template
                                                        [[:.clj-dashboard-comment-item html/first-of-type]]))

(def dashboard-comments-no-comments-snippet (html/select pf/library-html-resource
                                                         [:.clj-library-key--dashboard-no-comment-item]))

(def comments-without-writer-note-snippet (html/select pf/library-html-resource [:.clj-library-key--dashboard-comment-without-writer-note]))

(def comments-with-writer-note-snippet (html/select pf/library-html-resource [:.clj-library-key--dashboard-comment-with-writer-note]))

(def no-writer-note-snippet (html/select comments-without-writer-note-snippet [:.clj-dashboard-comment-item]))

(def writer-note-snippet (html/select comments-with-writer-note-snippet [:.clj-dashboard-comment-item]))

(defn dashboard-comments-no-comments [{:keys [translations data] :as context}]
  (let [translation-key (case (:comment-view-type data)
                          :paperclip :writer-dashboard/no-comments-with-writer-notes-message
                          :writer-dashboard/no-comments-message)]
    (html/at dashboard-comments-no-comments-snippet
             [:.clj-dashboard-no-comment-item] (html/content (translations translation-key)))))

(defn apply-writer-note-form-validations [{:keys [doc] :as context} comment nodes]
  (let [validation-data (get-in doc [:flash :validation])
        validation-report (:report validation-data)
        previous-inputs (:data validation-data)
        is-relevant-comment (= (:uri comment)
                               (:note-on-uri previous-inputs))]
    (if is-relevant-comment
      (html/at nodes
               [:.clj-writer-note-empty-error] (when (contains? (:note validation-report) :empty) identity)
               [:.clj-writer-note-item-field] (html/set-attr :value (:note previous-inputs)))
      (html/at nodes
               [:.clj-writer-note-empty-error] nil))))

(defn render-comment-without-note [{:keys [ring-request] :as context} comment]
  (->> (html/at no-writer-note-snippet
                [:.clj-dashboard-comment-text] (html/content (:comment comment))
                [:.clj-dashboard-comment-author] (html/content (:username comment))
                [:.clj-dashboard-comment-date] (html/content (utils/iso-time-string->pretty-time (:_created_at comment)))
                [:.clj-dashboard-comment-up-count] (html/content (str (get-in comment [:votes :up])))
                [:.clj-dashboard-comment-down-count] (html/content (str (get-in comment [:votes :down])) )
                [:.clj-refer] (html/set-attr :value (utils/referer-url ring-request))
                [:.clj-note-on-uri] (html/set-attr :value (:uri comment))
                [:.clj-dashboard-writer-note-form] (html/prepend (html/html-snippet (anti-forgery-field))))
       (apply-writer-note-form-validations context comment)))

(defn render-comment-with-note [context comment]
 (html/at writer-note-snippet
          [:.clj-dashboard-comment-text] (html/content (:comment comment))
          [:.clj-dashboard-comment-author] (html/content (:username comment))
          [:.clj-dashboard-comment-date] (html/content (utils/iso-time-string->pretty-time (:_created_at comment)))
          [:.clj-dashboard-comment-up-count] (html/content (str (get-in comment [:votes :up])))
          [:.clj-dashboard-comment-down-count] (html/content (str (get-in comment [:votes :down])))
          [:.clj-dashboard-writer-note-text] (html/content (:note comment))))

(defn comment-list-items [{:keys [data] :as context}]
  (let [comments (:comments data)]
    (html/at comments-without-writer-note-snippet
             [:.clj-dashboard-comment-item]
             (html/clone-for [comment comments]
                             [:.clj-dashboard-comment-item] (if (:note comment)
                                                             (html/substitute (render-comment-with-note context comment))
                                                             (html/substitute (render-comment-without-note context comment)))))))

(defn comment-list [{:keys [data] :as context}]
  (let [comments (:comments data)]
    (if (empty? comments)
      (dashboard-comments-no-comments context)
      (comment-list-items context))))

(def dashboard-comments-navigation-item-snippet (html/select dashboard-comments-template
                                                             [[:.clj-dashboard-navigation-item html/first-of-type]]))

(defn draft-label [{:keys [translations] :as context} draft]
  (str (translations :dashboard-comments/draft-label-prefix) ": " (utils/iso-time-string->pretty-time (:_created_at draft))))

(defn draft->navigation-list-item [{:keys [translations] :as context} draft]
  {:label (draft-label context draft)
   :link-count (get-in draft [:meta :comments-count])
   :uri (:uri draft)})

(defn objective->navigation-list-item [{:keys [translations] :as context} objective]
  {:label (translations :dashboard-comments/objective-label)
   :link-count (get-in objective [:meta :comments-count])
   :uri (:uri objective)})

(defn navigation-list [{:keys [data translations] :as context}]
  (let [objective (:objective data)
        objective-nav-item (objective->navigation-list-item context objective)
        draft-nav-items (mapv (partial draft->navigation-list-item context) (:drafts data))
        navigation-list-items (cond-> draft-nav-items
                                (not (empty? draft-nav-items)) (update-in [0 :label]
                                                                          #(str % " (" (translations :dashboard-comments/latest-draft-label) ")"))
                                true                           (conj objective-nav-item))
        selected-comment-target-uri (:selected-comment-target-uri data)
        dashboard-url (url/url (utils/path-for :fe/dashboard-comments :id (:_id objective)))]
    (html/at dashboard-comments-navigation-item-snippet
             [:.clj-dashboard-navigation-item]
             (html/clone-for [item navigation-list-items]
                             [:.clj-dashboard-navigation-item] (if (= selected-comment-target-uri (:uri item))
                                                                 (html/add-class "on")
                                                                 identity)
                             [:.clj-dashboard-navigation-item-label] (html/content (:label item))
                             [:.clj-dashboard-navigation-item-link-count] (when (:link-count item) (html/content (str "(" (:link-count item) ")")))
                             [:.clj-dashboard-navigation-item-link]
                             (html/set-attr :href
                                            (str (assoc dashboard-url
                                                        :query {:selected (:uri item)}
                                                        :anchor "dashboard-content")))))))

(defn dashboard-comments [{:keys [doc data] :as context}]
  (let [objective (:objective data)
        selected-comment-target-uri (:selected-comment-target-uri data)
        dashboard-url (url/url (utils/path-for :fe/dashboard-comments :id (:_id objective)))
        comment-view-type (:comment-view-type data)]
    (apply str
           (html/emit*
            (tf/translate context
                          (pf/add-google-analytics
                           (html/at dashboard-comments-template
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                    [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

                                    [:.clj-dashboard-title-link] (html/set-attr :href (url/url (utils/path-for :fe/objective :id (:_id objective))))
                                    [:.clj-dashboard-title-link] (html/content (:title objective))

                                    [:.clj-dashboard-stat-participant] nil
                                    [:.clj-dashboard-stat-starred-amount] (html/content (str (get-in objective [:meta :stars-count])))
                                    [:.clj-writer-dashboard-navigation-questions-link] (html/set-attr :href (utils/path-for :fe/dashboard-questions :id (:_id objective)))
                                    [:.clj-writer-dashboard-navigation-comments-link] (html/set-attr :href (utils/path-for :fe/dashboard-comments :id (:_id objective)))
                                    [:.clj-dashboard-navigation-list] (html/content (navigation-list context))
                                    [:.clj-dashboard-comment-list] (html/substitute (comment-list context))

                                    [:.clj-dashboard-filter-paper-clip] (html/set-attr
                                                                       :href
                                                                       (str (assoc dashboard-url
                                                                                   :query {:selected selected-comment-target-uri
                                                                                           :comment-view "paperclip"})))
                                    [:.clj-dashboard-filter-up-votes] (html/set-attr
                                                                       :href
                                                                       (str (assoc dashboard-url
                                                                                   :query {:selected selected-comment-target-uri
                                                                                           :comment-view "up-votes"})))
                                    
                                    [:.clj-dashboard-filter-down-votes] (html/set-attr
                                                                         :href
                                                                         (str (assoc dashboard-url
                                                                                     :query {:selected selected-comment-target-uri
                                                                                             :comment-view "down-votes"})))

                                    [:.clj-dashboard-filter-paper-clip] (if (= comment-view-type :paperclip)
                                                                        (html/add-class "on")
                                                                        identity)

                                    [:.clj-dashboard-filter-up-votes] (if (= comment-view-type :up-votes)
                                                                        (html/add-class "on")
                                                                        identity)

                                    [:.clj-dashboard-filter-down-votes] (if (= comment-view-type :down-votes)
                                                                          (html/add-class "on")
                                                                          identity)
                                    [:.clj-dashboard-content-stats] nil)))))))
