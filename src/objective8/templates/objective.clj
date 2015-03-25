(ns objective8.templates.objective
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.templates.page-furniture :as f]))   

(def objective-template (html/html-resource "templates/jade/objective.html" {:parser jsoup/parser}))

(defn objective-page [{:keys [translations data] :as context}]
  (let [objective (:objective data)
        candidates (:candidates data)]
    (apply str
           (html/emit*
             (html/at objective-template
                      [:title] (html/content (get-in context [:doc :title]))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-status-bar] (html/substitute (f/status-flash-bar context))
                      [:.clj-objective-progress-indicator] nil
                      [:.clj-guidance-buttons] nil
                      [:.clj-guidance-heading] (html/content (translations :objective-guidance/heading))
                      [:.clj-guidance-text-line-1] (html/content (translations :objective-guidance/text-line-1))
                      [:.clj-guidance-text-line-2] (html/content (translations :objective-guidance/text-line-2))
                      [:.clj-guidance-text-line-3] (html/content (translations :objective-guidance/text-line-3))

                      [:.clj-objective-navigation-item-objective] (html/content (translations :objective-nav/objective))
                      [:.clj-objective-navigation-item-writers] (html/content (translations :objective-nav/writers))
                      [:.clj-objective-navigation-item-questions] (html/content (translations :objective-nav/questions))
                      [:.clj-objective-navigation-item-comments] (html/content (translations :objective-nav/comments))

                      [:.clj-objective-title] (html/content (:title objective))
                      [:.clj-drafting-started-wrapper] (html/substitute (f/drafting-message context))
                      [:.clj-replace-with-objective-detail] (html/substitute (f/text->p-nodes (:description objective)))
                      [:.clj-writers-section-title] (html/content (translations :objective-view/writers))
                      [:.clj-writer-item-list] (html/content (f/writer-list context)) 
                      [:.clj-invite-writer-link] (html/do-> 
                                                   (html/set-attr "href" (str "/objectives/" (:_id objective) 
                                                                              "/invite-writer"))
                                                   (html/content (translations :objective-view/invite-a-writer)))
                      [:.clj-questions-section-title] (html/content (translations :objective-view/questions-title))
                      [:.clj-question-list] (html/content (f/question-list context)) 
                      [:.clj-ask-question-link] (html/do-> 
                                                   (html/set-attr "href" (str "/objectives/" (:_id objective) 
                                                                              "/add-question"))
                                                   (html/content (translations :objective-view/ask-a-question)))
                      [:.clj-comments-section-title] (html/content (translations :objective-view/comments))
                      [:.clj-comment-list] (html/content (f/comment-list context))
                      [:.clj-comment-create] (when-not (:drafting-started objective)
                                               (html/content (f/comment-create context))))))))
