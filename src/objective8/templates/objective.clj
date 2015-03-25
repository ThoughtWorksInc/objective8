(ns objective8.templates.objective
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.templates.page-furniture :as f]))   

(def objective-template (html/html-resource "templates/jade/objective.html" {:parser jsoup/parser}))

(defn objective-page [{:keys [translations data doc] :as context}]
  (let [objective (:objective data)
        candidates (:candidates data)]
    (apply str
           (html/emit*
             (html/at objective-template
                      [:title] (html/content (:title doc))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-status-bar] (html/substitute (f/status-flash-bar context))
                      [:.clj-objective-progress-indicator] nil
                      [:.clj-guidance-buttons] nil
                      [:.clj-guidance-heading] (html/content (translations :objective-guidance/heading))
                      [:.l8n-guidance-text-line-1] (html/content (translations :objective-guidance/text-line-1))
                      [:.l8n-guidance-text-line-2] (html/content (translations :objective-guidance/text-line-2))
                      [:.l8n-guidance-text-line-3] (html/content (translations :objective-guidance/text-line-3))

                      [:.l8n-objective-navigation-item-objective] (html/content (translations :objective-nav/objective))
                      [:.l8n-objective-navigation-item-writers] (html/content (translations :objective-nav/writers))
                      [:.l8n-objective-navigation-item-questions] (html/content (translations :objective-nav/questions))
                      [:.l8n-objective-navigation-item-comments] (html/content (translations :objective-nav/comments))

                      [:.clj-star-container] nil

                      [:.clj-objective-title] (html/content (:title objective))
                      [:.clj-drafting-started-wrapper] (html/substitute (f/drafting-message context))
                      [:.clj-replace-with-objective-detail] (html/substitute (f/text->p-nodes (:description objective)))
                      [:.l8n-writers-section-title] (html/content (translations :objective-view/writers))
                      [:.clj-writer-item-list] (html/content (f/writer-list context)) 
                      [:.clj-invite-writer-link] (html/set-attr 
                                                   "href" (str "/objectives/" (:_id objective) 
                                                               "/invite-writer"))
                      [:.l8n-invite-writer-link] (html/content (translations :objective-view/invite-a-writer))
                      [:.l8n-questions-section-title] (html/content (translations :objective-view/questions-title))
                      [:.clj-question-list] (html/content (f/question-list context)) 
                      [:.clj-ask-question-link] (html/set-attr "href" (str "/objectives/" (:_id objective) 
                                                                              "/add-question"))
                      [:.l8n-ask-question-link] (html/content (translations :objective-view/ask-a-question))
                      [:.l8n-comments-section-title] (html/content (translations :objective-view/comments))
                      [:.clj-comment-list] (html/content (f/comment-list context))
                      [:.clj-comment-create] (when-not (:drafting-started objective)
                                               (html/content (f/comment-create context))))))))
