(ns objective8.templates.objective
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [objective8.utils :as utils]     
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))   

(def objective-template (html/html-resource "templates/jade/objective.html" {:parser jsoup/parser}))


(defn mail-to-string [flash objective translations]
  (str "mailto:" (:writer-email flash)
       "?subject=" (translations :invitation-modal/email-subject)
       "&body=" (translations :invitation-modal/email-body-line-1) " " (:title objective)
       "%0d%0d" (translations :invitation-modal/email-body-line-2) "%0d" (:invitation-url flash)))

(defn writer-invitation-modal [flash objective translations]
  (html/transformation
    [:.l8n-invitation-guidance-text-line-1] (html/content (translations :invitation-modal/guidance-text-line-1))
    [:.clj-invitation-url] (html/set-attr "value" (:invitation-url flash))
    [:.clj-mail-to] (html/set-attr :href (mail-to-string flash
                                                         objective
                                                         translations))))

(def invitation-response-snippet (html/select (html/html-resource "templates/jade/objective-invitation-response.html") [:.clj-invitation-response]))

(defn invitation-rsvp-modal [{:keys [data invitation-rsvp user ring-request] :as context}]
  (let [objective (:objective data)
        tl8 (tf/translator context)
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
                                          (html/substitute (html/at pf/anchor-button 
                                                                    [:.clj-anchor-button] (html/do-> 
                                                                                            (html/set-attr :href (str "/sign-in?refer=" (:uri ring-request)))
                                                                                            (tl8 :invitation-response/sign-in-to-accept))))))))))

;; DRAFTING HAS STARTED MESSAGE

(def drafting-message-snippet (html/select pf/library-html-resource [:.clj-drafting-message]))

(defn drafting-message [{:keys [data] :as context}]
  (let [objective (:objective data)]
    (html/at drafting-message-snippet
      [:.clj-drafting-message-link] (html/set-attr :href (str "/objectives/" (:_id objective) 
                                                              "/drafts")))))

(defn drafting-begins [objective]
  (html/transformation
    [:.clj-days-left-day] (html/do->
                            (html/set-attr :drafting-begins-date (:end-date objective))
                            (html/content (str (:days-until-drafting-begins objective))))))

(defn invitation-rsvp-for-objective? [objective invitation-rsvp]
  (let [objective-id (:_id objective)
        invitation-objective-id (:objective-id invitation-rsvp)]
    (and objective-id (= invitation-objective-id objective-id))))

;; QUESTION LIST

(def empty-question-list-item-snippet (html/select pf/library-html-resource [:.clj-empty-question-list-item]))

(def question-list-item-snippet (html/select pf/library-html-resource [:.clj-question-item]))

(defn question-list-items [questions]
  (html/at question-list-item-snippet
           [:.clj-question-item] 
           (html/clone-for [question questions]
                           [:.clj-question-text] (html/content (:question question))
                           [:.clj-answer-link] (html/set-attr :href (str "/objectives/" (:objective-id question)
                                                                         "/questions/" (:_id question))))))


(defn question-list [{:keys [data] :as context}]
  (let [questions (:questions data)]
    (if (empty? questions)
      empty-question-list-item-snippet
      (question-list-items questions))))

;; STAR FORM
(defn star-form-when-signed-in [{:keys [data ring-request] :as context}]
      (html/transformation
             [:.clj-star-form] (html/prepend (html/html-snippet (anti-forgery-field)))
             [:.clj-refer] (html/set-attr :value (:uri ring-request))
             [:.clj-star-on-uri] (html/set-attr :value (:uri ring-request))
             [:.clj-objective-star] (if (tf/starred? (:objective data))
                                      (html/add-class "starred")
                                      identity)))

(defn star-form-when-not-signed-in [{:keys [ring-request] :as context}]
  (html/transformation
             [:.clj-star-form] (html/set-attr :method "get")
             [:.clj-star-form] (html/set-attr :action "/sign-in")
             [:.clj-refer] (html/set-attr :value (:uri ring-request))
             [:.clj-star-on-uri] nil))

;; OBJECTIVE PAGE
(def star-form-snippet (html/select objective-template [:.clj-star-form]))

(defn star-form [objective ring-request]
  (html/at star-form-snippet
           [:.clj-star-form] (html/prepend (html/html-snippet (anti-forgery-field)))
           [:.clj-refer] (html/set-attr :value (:uri ring-request))
           [:.clj-star-on-uri] (html/set-attr :value (:uri ring-request))
           [:.clj-objective-star] (if (tf/starred? objective)
                                    (html/add-class "starred")
                                    identity)))

(defn objective-page [{:keys [translations data doc invitation-rsvp ring-request user] :as context}]
  (let [objective (:objective data)
        objective-id (:_id objective)
        candidates (:candidates data)
        flash (:flash doc)
        optionally-disable-voting (if (tf/in-drafting? objective)
                                    (pf/disable-voting-actions translations)
                                    identity)]
    (apply str
           (html/emit*
             (tf/translate context
                           (pf/add-google-analytics
                             (html/at objective-template
                                      [:title] (html/content (:title doc))
                                      [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                      [:.clj-status-bar] (html/substitute 
                                                           (pf/status-flash-bar
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

                                      [:.clj-star-form] (if user
                                                          (star-form-when-signed-in context)
                                                          (star-form-when-not-signed-in context))

                                      [:.clj-objective-title] (html/content (:title objective))

                                      [:.clj-days-left] (when (tf/open? objective)
                                                          (drafting-begins objective))
                                      [:.clj-drafting-started-wrapper] (when (tf/in-drafting? objective)
                                                                         (html/substitute (drafting-message context)))
                                      [:.clj-replace-with-objective-detail] (html/substitute (tf/text->p-nodes (:description objective)))

                                      [:.clj-writer-item-list] (html/content (pf/writer-list context))
                                      [:.clj-invite-writer-link] (when (and 
                                                                         (utils/writer-inviter-for? user objective-id) 
                                                                         (tf/open? objective)) 
                                                                   (html/set-attr
                                                                     :href (str "/objectives/" (:_id objective) "/invite-writer")))

                                      [:.clj-question-list] (html/content (question-list context))
                                      [:.clj-ask-question-link] (when (tf/open? objective)
                                                                  (html/set-attr
                                                                    "href" (str "/objectives/" (:_id objective) "/add-question")))

                                      [:.clj-comment-list] (html/content
                                                             (optionally-disable-voting
                                                               (pf/comment-list context)))
                                      [:.clj-comment-create] (when (tf/open? objective)
                                                               (html/content (pf/comment-create context :objective))))))))))
