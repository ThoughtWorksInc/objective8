(ns objective8.templates.question
  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :as f]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(def question-template (html/html-resource "templates/jade/question.html"))

(defn question-page [{:keys [data user ring-request] :as context}]
  (let [question (:question data)
        answers (:answers data)
        objective (:objective data)]
    (prn objective)
    (apply str
           (html/emit*
             (html/at question-template
                      [:title] (html/content (get-in context [:doc :title]))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-objective-link] (html/set-attr "href" (str "/objectives/" (:_id objective)))
                      [:.clj-objective-link html/text-node] (constantly (:title objective)) 
                      [[ html/text-node (html/left :a.clj-objective-link)]] (constantly (str " > " (:question question)))
                      [:.clj-question] (html/content (:question question))
                      [:.clj-answer] (html/clone-for [answer answers]
                                                     [:.clj-answer-text] (html/content (:answer answer)))
                      [:.clj-answer-form] (if user
                                            identity 
                                            (html/content "!You must sign in to add an answer"))
                      [:.clj-answer-form] (html/set-attr
                                            "action"
                                            (str "/objectives/" (:_id objective) "/questions/" (:_id question) "/answers"))
                      [:.clj-answer-form] (html/prepend
                                            (html/html-snippet (anti-forgery-field))))))))

