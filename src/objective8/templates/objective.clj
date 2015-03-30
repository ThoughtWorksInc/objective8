(ns objective8.templates.objective
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [objective8.utils :as utils]     
            [objective8.templates.page-furniture :as f]))   

(def objective-template (html/html-resource "templates/jade/objective.html" {:parser jsoup/parser}))


(defn mail-to-string [flash objective translations]
  (str "mailto:" (:writer-email flash)
       "?subject=" (translations :invitation-modal/email-subject)
       "&body=" (translations :invitation-modal/email-body-line-1) " " (:title objective)
       "%0d%0d" (translations :invitation-modal/email-body-line-2) "%0d" (:invitation-url flash)))

(defn writer-invitation-modal [flash objective translations]
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

(def invitation-response-snippet (html/select (html/html-resource "templates/jade/objective-invitation-response.html") [:.clj-invitation-response]))

(defn invitation-rsvp-modal [{:keys [data invitation-rsvp user ring-request] :as context}]
  (let [objective (:objective data)
        tl8 (f/translator context)
        objective-id (:objective-id invitation-rsvp)
        invitation-id (:invitation-id invitation-rsvp)]
    (html/transformation
      [:.clj-modal-contents] (html/content

      (html/at invitation-response-snippet
               [:.l8n-invitation-response-title] (tl8 :invitation-response/page-title)
               [:.l8n-invitation-response-help-achieve] (tl8 :invitation-response/help-achieve)
               [:.clj-objective-title] (html/content (:title objective)) 
               [:.l8n-rsvp-text] (tl8 :invitation-response/rsvp-text)
               
               [:.clj-invitation-response-decline] 
               (html/do-> 
                 (html/set-attr :action (utils/local-path-for :fe/decline-invitation :id (:objective-id invitation-rsvp) :i-id (:invitation-id invitation-rsvp)))
                 (html/prepend (html/html-snippet (anti-forgery-field)))) 

               [:.l8n-invitation-decline-text] (tl8 :invitation-response/decline)
               [:.l8n-invitation-accept-text] (tl8 :invitation-response/accept)     

               [:.clj-invitation-response-accept] 
               (if user
                 (html/do-> (html/prepend (html/html-snippet (anti-forgery-field))) 
                            (html/set-attr :action (utils/local-path-for :fe/accept-invitation :id (:objective-id invitation-rsvp) :i-id (:invitation-id invitation-rsvp))))
                 (html/substitute (html/at f/anchor-button 
                                           [:.clj-anchor-button] (html/do-> 
                                                                   (html/set-attr :href (str "/sign-in?refer=" (:uri ring-request)))
                                                                   (tl8 :invitation-response/sign-in-to-accept))))))))))

(defn drafting-begins [objective translations]
  (html/transformation
    [:.l8n-days-left-head] (html/content (translations :objective-view/drafting-begins))
    [:.clj-days-left-day] (html/do->
                            (html/set-attr "drafting-begins-date"
                                           (:end-date objective))
                            (html/content (str (:days-until-drafting-begins objective))))
    [:.l8n-days-left-foot] (html/content (str " " (translations :objective-view/days)))))

(defn invitation-rsvp-for-objective? [objective invitation-rsvp]
  (let [objective-id (:_id objective)
        invitation-objective-id (:objective-id invitation-rsvp)]
    (and objective-id (= invitation-objective-id objective-id))))

(defn objective-page [{:keys [translations data doc invitation-rsvp] :as context}]
  (let [objective (:objective data)
        objective-id (:_id objective)
        candidates (:candidates data)
        flash (:flash doc)]
    (apply str
           (html/emit*
             (html/at objective-template
                      [:title] (html/content (:title doc))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-status-bar] (html/substitute 
                                           (f/status-flash-bar
                                             (cond
                                               (= :invitation (:type flash)) (update-in context [:doc] dissoc :flash)
                                               (invitation-rsvp-for-objective? objective invitation-rsvp) (dissoc context :invitation-rsvp)
                                               :else context)))
                      [:.clj-writer-invitation] 
                      (if (= :invitation (:type flash))
                        (writer-invitation-modal flash objective translations)
                        (when (invitation-rsvp-for-objective? objective invitation-rsvp) 
                          (invitation-rsvp-modal context)))

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
