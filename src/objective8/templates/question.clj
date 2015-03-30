(ns objective8.templates.question
  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :as f]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(def question-template (html/html-resource "templates/jade/question.html"))

(defn question-page [{:keys [translations data user ring-request doc] :as context}]
  (let [question (:question data)
        answers (:answers data)
        objective (:objective data)
        tl8 (f/translator context)]
    (apply str
           (html/emit*
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
                                                     [:.clj-approval-form] (html/prepend (html/html-snippet (anti-forgery-field)))
                                                     [:.clj-answer-text] (html/content (:answer answer))
                                                     [:.clj-vote-on-uri] (html/set-attr "value" (:uri answer))
                                                     [:.clj-refer] (html/set-attr "value" (str "/objectives/" (:_id objective) "/questions/" (:_id question)))
                                                     [:.clj-up-score] (html/content (str (get-in answer [:votes :up])))
                                                     [:.clj-down-score] (html/content (str (get-in answer [:votes :down]))))

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
                      [:.l8n-answers-for] (tl8 :answer-create/answers-for))))))
