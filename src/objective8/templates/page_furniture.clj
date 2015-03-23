(ns objective8.templates.page-furniture
  (:require [net.cgrand.enlive-html :as html]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [objective8.utils :as utils]))

(def library-html "templates/jade/library.html")

(defn text->p-nodes
  "Turns text into a collection of paragraph nodes based on linebreaks.
   Returns nil if no text is supplied"
  [text]
  (when text
    (let [newline-followed-by-optional-whitespace #"(\n+|\r+)\s*"]
    (map (fn [p] (html/html [:p p])) (clojure.string/split text
                                                           newline-followed-by-optional-whitespace)))))

;; MASTHEAD

(html/defsnippet masthead-signed-in library-html [:.clj-masthead-signed-in] [] identity)

(html/defsnippet masthead
  library-html [:.clj-masthead-signed-out] [{{uri :uri} :ring-request
                                                              :keys [translations  user] :as context}]
  [:.clj-masthead-signed-out] (if user
                                (html/substitute (masthead-signed-in))
                                identity)
  [:.clj-masthead-skip-text] (html/content (translations :masthead/skip-to-navigation))
  [:.clj-masthead-logo] (html/set-attr "title" (translations :masthead/logo-title-attr))
  [:.clj-masthead-objectives-link] (html/do->
                                     (html/set-attr "title" (translations :masthead/objectives-link-title-attr))
                                     (html/content (translations :masthead/objectives-link)))
  [:.clj-masthead-about-link] (html/do->
                                (html/set-attr "title" (translations :masthead/about-link-title-attr))
                                (html/content (translations :masthead/about-link)))
  [:.clj-masthead-sign-in] (html/set-attr "title" (translations :navigation-global/sign-in-title))
  [:.clj-masthead-sign-in] (html/set-attr "href" (str "/sign-in?refer=" uri))
  [:.clj-masthead-sign-in-text] (html/content (translations :navigation-global/sign-in-text))
  [:.clj-masthead-sign-out] (html/set-attr "title" (translations :navigation-global/sign-out-title))
  [:.clj-masthead-sign-out-text] (html/content (translations :navigation-global/sign-out-text))
  [:.clj-username] (html/content (:username user)))

;; STATUS BAR

(html/defsnippet flash-bar library-html [:.clj-flash-message-bar] [flash]
  [:.clj-flash-message-bar-text] (html/content flash))

(html/defsnippet status-flash-bar
  library-html [:.clj-status-bar] [{:keys [doc translations] :as context}]
  [:.clj-status-bar] (if-let [flash (:flash doc)] 
                       (html/substitute (flash-bar flash))
                       identity)
  [:.clj-status-bar-text] (html/content (translations :status-bar/status-text)))


;; DRAFTING HAS STARTED MESSAGE

(html/defsnippet drafting-message library-html [:.clj-drafting-message] [{{objective :objective} :data
                                                                         translations :translations
                                                                         :as context}]
  [html/any-node] (when (:drafting-started objective) identity)
  [:.clj-drafting-message-title] (html/content (translations :notifications/drafting-message-title))
  [:.clj-drafting-message-body] (html/content (translations :notifications/drafting-message-body))
  [:.clj-drafting-message-link] (html/do->
                                  (html/set-attr "href" (str "/objectives/" (:_id objective) "/drafts/latest"))
                                  (html/content (translations :notifications/drafting-message-link))))

;; WRITER LIST

(html/defsnippet empty-writer-list-item
  library-html [:.clj-empty-writer-list-item] [{translations :translations}]
  [:.clj-empty-writer-list-item] (html/content (translations :candidate-list/no-candidates)))

(html/defsnippet writer-list-items
  library-html [:.clj-writer-item-without-photo] [candidates]
  [:.clj-writer-item-without-photo :a] nil
  [:.clj-writer-item-without-photo] (html/clone-for [candidate candidates]
                                                    [:.clj-writer-name] (html/content (:writer-name candidate))
                                                    [:.clj-writer-description] (html/content (:invitation-reason candidate))))

(defn writer-list [context]
  (let [candidates (get-in context [:data :candidates])]
    (if (empty? candidates)
      (empty-writer-list-item context)
      (writer-list-items candidates))))

;; QUESTION LIST

(html/defsnippet empty-question-list-item
  library-html [:.clj-empty-question-list-item] [{translations :translations}]
  [:.clj-empty-question-list-item] (html/content (translations :question-list/no-questions)))

(html/defsnippet question-list-items
  library-html [:.clj-question-item] [questions translations]
  [:.clj-question-item] (html/clone-for [question questions]
                                        [:.clj-question-text] (html/content (:question question))
                                        [:.clj-answer-link] (html/do->
                                                              (html/content (translations :objective-view/answer-link))
                                                              (html/set-attr "href" (str "/objectives/" (:objective-id question)
                                                                                         "/questions/" (:_id question))))))

(defn question-list [{translations :translations :as context}]
  (let [questions (get-in context [:data :questions])]
    (if (empty? questions)
      (empty-question-list-item context)
      (question-list-items questions translations))))

;; COMMENT LIST

(html/defsnippet empty-comment-list-item
  library-html [:.clj-empty-comment-list-item] [translations]
  [:.clj-empty-comment-list-item] (html/content (translations :comment-view/no-comments)))

(html/defsnippet comment-list-items
  library-html [:.clj-comment-item] [comments]
  [:.clj-comment-item] (html/clone-for [comment comments]
                                       [:.clj-comment-author] (html/content (:username comment))
                                       [:.clj-comment-date] (html/content (utils/iso-time-string->pretty-time (:_created_at comment)))
                                       [:.clj-comment-text] (html/content (:comment comment))
                                       [:.clj-comment-actions] nil))

(defn comment-list [{translations :translations :as context}]
  (let [comments (get-in context [:data :comments])]
    (if (empty? comments)
      (empty-comment-list-item translations)
      (comment-list-items comments))))

;; COMMENT CREATE

(html/defsnippet comment-create-form
  library-html [:.clj-add-comment-form] [{:keys [translations data]}]
  [:.clj-add-comment-form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:.clj-objective-id-input] (html/set-attr "value" (get-in data [:objective :_id]))
  [:.clj-comment-on-id-input] (html/set-attr "value" (get-in data [:objective :global-id]))
  [:.clj-add-comment] (html/content (translations :comment-create/post-button)))

(html/defsnippet sign-in-to-comment
  library-html [:.clj-please-sign-in] [{:keys [translations ring-request]}]
  [:.clj-before-link] (html/content (translations :comment-sign-in/please))
  [:.clj-sign-in-link] (html/do->
                         (html/set-attr "href" (str "/sign-in?refer=" (:uri ring-request)))
                         (html/content (translations :comment-sign-in/sign-in)))
  [:.clj-after-link] (html/content (translations :comment-sign-in/to)))

(defn comment-create [{user :user :as context}]
  (if user
    (comment-create-form context)
    (sign-in-to-comment context))) 
