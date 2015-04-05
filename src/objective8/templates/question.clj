(ns objective8.templates.question
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))

(def question-template (html/html-resource "templates/jade/question.html" {:parser jsoup/parser}))

(defn voting-actions-when-signed-in [{:keys [ring-request] :as context} answer]
  (html/transformation
    [:.clj-approval-form] (html/prepend (html/html-snippet (anti-forgery-field)))
    [:.clj-vote-on-uri] (html/set-attr :value (:uri answer))
    [:.clj-refer] (html/set-attr :value (:uri ring-request))
    [:.clj-up-score] (html/content (str (get-in answer [:votes :up])))
    [:.clj-down-score] (html/content (str (get-in answer [:votes :down])))))

(defn voting-actions-when-signed-out [{:keys [ring-request] :as context} answer]
  (html/transformation
    [:.clj-approval-form] (html/set-attr :method "get")
    [:.clj-approval-form] (html/set-attr :action "/sign-in")
    [:.clj-vote-on-uri] nil
    [:.clj-refer] (html/set-attr :value (:uri ring-request))
    [:.clj-up-score] (html/content (str (get-in answer [:votes :up])))
    [:.clj-down-score] (html/content (str (get-in answer [:votes :down])))))

(defn disable-voting [translations]
  (html/transformation
   [:.clj-approval-button] (comp
                            (html/set-attr :disabled "disabled")
                            (html/set-attr :title (translations :answer-votes/drafting-started)))))

(defn question-page [{:keys [translations data user doc] :as context}]
  (let [question (:question data)
        answers (:answers data)
        objective (:objective data)
        tl8 (tf/translator context)
        optionally-disable-voting (if (tf/in-drafting? (:objective data))
                                    (disable-voting translations)
                                    identity)]
    (apply str
           (html/emit*
             (tf/translate context
                           (pf/add-google-analytics
                             (html/at question-template
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                      [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                      [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                                      [:.clj-objective-link] (html/set-attr :href (str "/objectives/" (:_id objective)))
                                      [:.clj-objective-link] (html/content (:title objective)) 
                                      [:.clj-question-breadcrumb] (html/content (:question question))

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
                                                                     (html/set-attr :action
                                                                                    (str "/objectives/" (:_id objective)
                                                                                         "/questions/" (:_id question) "/answers"))
                                                                     (html/prepend (html/html-snippet (anti-forgery-field))))
                                                            (html/substitute (pf/sign-in-to-add-answer context)))

                                      [:.l8n-guidance-heading] (tl8 :question-page/guidance-heading))))))))
