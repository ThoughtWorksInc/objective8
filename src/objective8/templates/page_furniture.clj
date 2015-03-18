(ns objective8.templates.page-furniture
  (:require [net.cgrand.enlive-html :as html]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.utils :as utils]))

(defn text->p-nodes
  "Turns text into a collection of paragraph nodes based on linebreaks.
   Returns nil if no text is supplied"
  [text]
  (when text
    (let [newline-followed-by-optional-whitespace #"(\n+|\r+)\s*"]
    (map (fn [p] (html/html [:p p])) (clojure.string/split text
                                                           newline-followed-by-optional-whitespace)))))

;; USER NAVIGATION

(html/defsnippet user-navigation-signed-in
  "templates/jade/library.html" [:.clj-user-navigation-signed-in] [{:keys [translations user]}]
  [:.clj-masthead-sign-out] (html/set-attr "title" (translations :navigation-global/sign-out-title))
  [:.clj-masthead-sign-out-text] (html/content (translations :navigation-global/sign-out-text))
  [:.clj-username] (html/content (:username user)))

(html/defsnippet user-navigation-signed-out
  "templates/jade/library.html" [:.clj-user-navigation-signed-out] [{{uri :uri} :ring-request :keys [translations user]}]
  [:.clj-masthead-sign-in] (html/set-attr "title" (translations :navigation-global/sign-in-title))
  [:.clj-masthead-sign-in] (html/set-attr "href" (str "/sign-in?refer=" uri))
  [:.clj-masthead-sign-in-text] (html/content (translations :navigation-global/sign-in-text)))

(defn user-navigation-signed-in? [{user :user :as context}]
  (if user
    (user-navigation-signed-in context) 
    (user-navigation-signed-out context)))

;; WRITER LIST

(html/defsnippet empty-writer-list-item
  "templates/jade/library.html" [:.clj-empty-writer-list-item] [{translations :translations}]
  [:.clj-empty-writer-list-item] (html/content (translations :candidate-list/no-candidates)))

(html/defsnippet writer-list-items
  "templates/jade/library.html" [:.clj-writer-item-without-photo] [candidates]
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
  "templates/jade/library.html" [:.clj-empty-question-list-item] [{translations :translations}]
  [:.clj-empty-question-list-item] (html/content (translations :question-list/no-questions)))

(html/defsnippet question-list-items
  "templates/jade/library.html" [:.clj-question-item] [questions translations]
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
  "templates/jade/library.html" [:.clj-empty-comment-list-item] [translations]
  [:.clj-empty-comment-list-item] (html/content (translations :comment-view/no-comments)))

(html/defsnippet comment-list-items
  "templates/jade/library.html" [:.clj-comment-item] [comments]
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
  "templates/jade/library.html" [:.clj-add-comment-form] [{:keys [translations data]}]
  [:.clj-add-comment-form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:.clj-objective-id-input] (html/set-attr "value" (get-in data [:objective :_id]))
  [:.clj-add-comment] (html/content (translations :comment-create/post-button)))

(html/defsnippet sign-in-to-comment  
  "templates/jade/library.html" [:.clj-please-sign-in] [{:keys [translations ring-request]}]
  [:.clj-before-link] (html/content (str (translations :comment-sign-in/please) " "))
  [:.clj-sign-in-link] (html/do-> 
                         (html/set-attr "href" (str "/sign-in?refer=" (:uri ring-request))) 
                         (html/content (translations :comment-sign-in/sign-in)))
  [:.clj-after-link] (html/content (str " " (translations :comment-sign-in/to))))

(defn comment-create [{user :user :as context}]
  (if user
    (comment-create-form context)
    (sign-in-to-comment context))) 
