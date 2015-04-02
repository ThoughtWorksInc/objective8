(ns objective8.templates.question
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as f]
            [objective8.templates.template-functions :as tf]
            ))

(def question-template (html/html-resource "templates/jade/question.html" {:parser jsoup/parser}))

(defn voting-actions-when-signed-in [{:keys [data ring-request] :as context} answer]
  (html/transformation
    [:.clj-approval-form] (html/prepend (html/html-snippet (anti-forgery-field)))
    [:.clj-vote-on-uri] (html/set-attr "value" (:uri answer))
    [:.clj-refer] (html/set-attr "value" (:uri ring-request))
    [:.clj-up-score] (html/content (str (get-in answer [:votes :up])))
    [:.clj-down-score] (html/content (str (get-in answer [:votes :down])))))

(defn voting-actions-when-signed-out [{:keys [data ring-request] :as context} answer]
  (html/transformation
    [:.clj-approval-form] (html/set-attr "method" "get")
    [:.clj-approval-form] (html/set-attr "action" "/sign-in")
    [:.clj-vote-on-uri] nil
    [:.clj-refer] (html/set-attr "value" (:uri ring-request))
    [:.clj-up-score] (html/content (str (get-in answer [:votes :up])))
    [:.clj-down-score] (html/content (str (get-in answer [:votes :down])))))

(defn disable-voting [translations]
  (html/transformation
   [:.clj-approval-button] (comp
                            (html/set-attr "disabled" "disabled")
                            (html/set-attr "title" (translations :answer-votes/drafting-started)))))

(defn question-page [{:keys [translations data user ring-request doc] :as context}]
  (let [question (:question data)
        answers (:answers data)
        objective (:objective data)
        tl8 (tf/translator context)
        optionally-disable-voting (if (tf/in-drafting? (:objective data))
                                    (disable-voting translations)
                                    identity)]
    (apply str
           (html/emit*
            (f/add-google-analytics
             (html/at question-template
                      [:title] (html/content (:title doc))
                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-status-bar] (html/substitute (f/status-flash-bar context))
                      [:.clj-objective-link] (html/set-attr "href" (str "/objectives/" (:_id objective)))
                      [:.clj-objective-link html/text-node] (constantly (:title objective)) 
                      [[html/text-node (html/left :a.clj-objective-link)]] (constantly (str " > " (:question question)))
                      [:.clj-question] (html/content (:question question))
                      [:.clj-answer] (html/clone-for [answer answers]
                                                     [:.clj-answer-text] (html/content (:answer answer))
                                                     [:.clj-approval-form] (comp
                                                                            optionally-disable-voting
                                                                            (if user
                                                                              (voting-actions-when-signed-in context answer)
                                                                              (voting-actions-when-signed-out context answer))))
                      [:.clj-jump-to-answer] (if user identity nil)

                      [:.clj-answer-form] (if user (html/do->
                                                    (html/set-attr "action"
                                                                   (str "/objectives/" (:_id objective)
                                                                        "/questions/" (:_id question) "/answers"))
                                                    (html/prepend (html/html-snippet (anti-forgery-field))))
                                              (html/substitute (f/sign-in-to-add-answer context)))

                      [:.l8n-guidance-heading] (tl8 :answer-create/guidance-heading)
                      [:.l8n-guidance-text-line-1] (tl8 :answer-create/guidance-text-1)
                      [:.l8n-submit-answer] (tl8 :answer-create/submit-answer-button)
                      [:.l8n-jump-to-answer] (tl8 :answer-create/jump-to-answer-button)
                      [:.l8n-answers-for] (tl8 :answer-create/answers-for)))))))
