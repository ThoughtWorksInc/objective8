(ns objective8.templates.question
  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :as f]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(def question-template (html/html-resource "templates/jade/question.html"))

(defn translator
  "Returns a translation function which replaces the
   content of nodes with translations for k"
  [{:keys [translations] :as context}]
  (fn [k] 
    #(assoc % :content (translations k))))

(defn question-page [{:keys [translations data user ring-request] :as context}]
  (let [question (:question data)
        answers (:answers data)
        objective (:objective data)
        tl8 (translator context)]
    (apply str
           (html/emit*
             (html/at question-template
                      [:title] (html/content (get-in context [:doc :title]))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-status-bar] (html/substitute (f/status-flash-bar context))
                      [:.clj-objective-link] (html/set-attr "href" (str "/objectives/" (:_id objective)))
                      [:.clj-objective-link html/text-node] (constantly (:title objective)) 
                      [[html/text-node (html/left :a.clj-objective-link)]] (constantly (str " > " (:question question)))
                      [:.clj-question] (html/content (:question question))
                      [:.clj-answer] (html/clone-for [answer answers]
                                                     [:.clj-answer-text] (html/content (:answer answer))
                                                     [:.clj-answer-id] (html/set-attr "value" (:global-id answer))
                                                     [:.clj-up-score] (constantly (str (get-in answer [:votes :up])))
                                                     [:.clj-down-score] (constantly (str (get-in answer [:votes :down]))))

                      ;TODO - re-enable approval options once redirect is working
                      [:.approval-options] (constantly nil)

                      [:.clj-jump-to-answer] (if user identity nil)

                      [:.clj-answer-form] (if user
                                            identity 
                                            (tl8 :answer-create/sign-in-reminder))
                      [:.clj-answer-form] (html/set-attr
                                            "action"
                                            (str "/objectives/" (:_id objective) "/questions/" (:_id question) "/answers"))
                      [:.clj-answer-form] (html/prepend
                                            (html/html-snippet (anti-forgery-field)))
                      
                      [:.l8n-guidance-heading] (tl8 :answer-create/guidance-heading)
                      [:.l8n-guidance-text-line-1] (tl8 :answer-create/guidance-text-1)
                      [:.l8n-submit-answer] (tl8 :answer-create/submit-answer-button)
                      [:.l8n-jump-to-answer] (tl8 :answer-create/jump-to-answer-button)
                      [:.l8n-answers-for] (tl8 :answer-create/answers-for))))))

