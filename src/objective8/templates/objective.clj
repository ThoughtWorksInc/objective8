(ns objective8.templates.objective
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.templates.page-furniture :as f]))   

(def objective-template (html/html-resource "templates/jade/objective.html" {:parser jsoup/parser}))

(defn mail-to-string [flash objective translations]
  (str "mailto:" (:writer-email flash)
       "?subject=" (translations :invitation-modal/email-subject)
       "&body=" (translations :invitation-modal/email-body-line-1) " " (:title objective)
       "%0d%0d" (translations :invitation-modal/email-body-line-2) "%0d" (:invitation-url flash)))

(defn writer-invitation [flash objective translations]
  (html/transformation
    [:.l8n-invitation-guidance-text-line-1] (html/content (translations :invitation-modal/guidance-text-line-1))
    [:.l8n-invitation-guidance-text-line-2] (html/content (translations :invitation-modal/guidance-text-line-2))
    [:.l8n-invitation-guidance-text-line-3] (html/content (translations :invitation-modal/guidance-text-line-3))
    [:.clj-invitation-url] (html/set-attr "value" (:invitation-url flash))
    [:.clj-mail-to] (html/do->
                      (html/set-attr "href" (mail-to-string flash
                                                            objective
                                                            translations))
                      (html/content (translations :invitation-modal/mail-to-text)))))

(defn drafting-begins [objective translations]
  (html/transformation
    [:.l8n-days-left-head] (html/content (translations :objective-view/drafting-begins))
    [:.clj-days-left-day] (html/do->
                            (html/set-attr "drafting-begins-date"
                                           (:end-date objective))
                            (html/content (str (:days-until-drafting-begins objective))))
    [:.l8n-days-left-foot] (html/content (str " " (translations :objective-view/days)))))

(defn objective-page [{:keys [translations data doc] :as context}]
  (let [objective (:objective data)
        candidates (:candidates data)
        flash (:flash doc)]
    (apply str
           (html/emit*
             (html/at objective-template
                      [:title] (html/content (:title doc))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-status-bar] (html/substitute (f/status-flash-bar
                                                            (if (= :invitation (:type flash))
                                                              (update-in context [:doc] dissoc :flash)
                                                              context)))
                      [:.clj-writer-invitation] (when (= :invitation (:type flash))
                                                  (writer-invitation flash objective translations))
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

                      [:.clj-days-left] (when-not (:drafting-started objective)
                                          (drafting-begins objective translations))
                      [:.clj-drafting-started-wrapper] (html/substitute (f/drafting-message context))
                      [:.clj-replace-with-objective-detail] (html/substitute (f/text->p-nodes (:description objective)))
                      [:.l8n-writers-section-title] (html/content (translations :objective-view/writers))
                      [:.clj-writer-item-list] (html/content (f/writer-list context))
                      [:.clj-invite-writer-link] (when-not (:drafting-started objective)
                                                   (html/set-attr
                                                     "href" (str "/objectives/" (:_id objective) "/invite-writer")))
                      [:.l8n-invite-writer-link] (when-not (:drafting-started objective)
                                                   (html/content
                                                     (translations :objective-view/invite-a-writer)))
                      [:.l8n-questions-section-title] (html/content (translations :objective-view/questions-title))
                      [:.clj-question-list] (html/content (f/question-list context))
                      [:.clj-ask-question-link] (when-not (:drafting-started objective)
                                                  (html/set-attr
                                                    "href" (str "/objectives/" (:_id objective) "/add-question")))
                      [:.l8n-ask-question-link] (when-not (:drafting-started objective)
                                                  (html/content
                                                    (translations :objective-view/ask-a-question)))
                      [:.l8n-comments-section-title] (html/content (translations :objective-view/comments))
                      [:.clj-comment-list] (html/content (f/comment-list context))
                      [:.clj-comment-create] (when-not (:drafting-started objective)
                                               (html/content (f/comment-create context :objective))))))))
