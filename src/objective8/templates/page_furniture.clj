(ns objective8.templates.page-furniture
  (:require [net.cgrand.enlive-html :as html]))

(defn text->p-nodes
  "Turns text into a collection of paragraph nodes based on linebreaks.
   Returns nil if no text is supplied"
  [text]
  (when text
    (let [newline-followed-by-optional-whitespace #"(\n+|\r+)\s*"]
    (map (fn [p] (html/html [:p p])) (clojure.string/split text
                                                           newline-followed-by-optional-whitespace)))))

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
