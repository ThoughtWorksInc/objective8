(ns objective8.templates.question
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [objective8.utils :as utils]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))

(def question-template (html/html-resource "templates/jade/question.html" {:parser jsoup/parser}))

(defn facebook-meta-tags [{:keys [data ring-request translations] :as context}]
  (let [question (:question data)]
    (html/transformation
     [:.clj-meta-sharing-facebook-title] (html/set-attr :content (:question question))
     [:.clj-meta-sharing-facebook-url] (html/set-attr :content (str utils/host-url (:uri ring-request)))
     [:.clj-meta-sharing-facebook-description] (html/set-attr :content (translations :question-page/facebook-description))
     [:.clj-meta-sharing-facebook-image] nil)))

(defn voting-actions-when-signed-in [{:keys [ring-request] :as context} answer]
  (html/transformation
    [:.clj-approval-form] (html/prepend (html/html-snippet (anti-forgery-field)))
    [:.clj-vote-on-uri] (html/set-attr :value (:uri answer))
    [:.clj-refer] (html/set-attr :value (str (:uri ring-request) "#answer-" (:_id answer)))
    [:.clj-up-score] (html/content (str (get-in answer [:votes :up])))
    [:.clj-down-score] (html/content (str (get-in answer [:votes :down])))))

(defn voting-actions-when-signed-out [{:keys [ring-request] :as context} answer]
  (html/transformation
    [:.clj-approval-form] (html/set-attr :method "get")
    [:.clj-approval-form] (html/set-attr :action "/sign-in")
    [:.clj-vote-on-uri] nil
    [:.clj-refer] (html/set-attr :value (str (:uri ring-request) "#answer-" (:_id answer)))
    [:.clj-up-score] (html/content (str (get-in answer [:votes :up])))
    [:.clj-down-score] (html/content (str (get-in answer [:votes :down])))))

(defn disable-voting [{:keys [translations] :as context}]
  (html/transformation
   [:.clj-approval-button] (comp
                            (html/set-attr :disabled "disabled")
                            (html/set-attr :title (translations :answer-votes/drafting-started)))))

(defn display-writer-note [answer]
  (html/transformation
    [:.clj-writer-note-item-content] (html/content (:note answer))))

(def sign-in-to-add-answer-snippet (html/select pf/library-html-resource [:.clj-to-add-answer-please-sign-in]))

(defn sign-in-to-add-answer [{:keys [ring-request] :as context}]
  (html/at sign-in-to-add-answer-snippet  
           [:.clj-to-add-answer-sign-in-link] 
           (html/set-attr :href (str "/sign-in?refer=" (:uri ring-request)))))

(defn apply-validations [{:keys [doc] :as context} nodes]
  (let [validation-data (get-in doc [:flash :validation])
        validation-report (:report validation-data)
        previous-inputs (:data validation-data)]
    (html/at nodes
             [:.clj-answer-length-error] (when (:answer validation-report) identity)
             [:.clj-input-answer] (html/content (:answer previous-inputs)))))

(defn question-page [{:keys [translations data user doc] :as context}]
  (let [question (:question data)
        answers (:answers data)
        objective (:objective data)
        tl8 (tf/translator context)
        optionally-disable-voting (if (tf/in-drafting? (:objective data))
                                    (disable-voting context)
                                    identity)]
    (apply str
           (html/emit*
             (tf/translate context
                           (pf/add-google-analytics
                             (apply-validations context
                             (html/at question-template
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                      [:head] (facebook-meta-tags context)
                                      [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                      [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                                      [:.clj-objective-link] (html/set-attr :href (str "/objectives/" (:_id objective)))
                                      [:.clj-objective-link] (html/content (:title objective)) 
                                      [:.clj-question-breadcrumb] (html/content (:question question))

                                      [:.clj-question] (html/content (:question question))
                                      [:.clj-answer] (html/clone-for [answer answers]
                                                                     [:.clj-answer] (html/set-attr :id (str "answer-" (:_id answer)))
                                                                     [:.clj-answer-text] (html/content (:answer answer))
                                                                     [:.clj-approval-form] (comp
                                                                                             optionally-disable-voting
                                                                                             (if user
                                                                                               (voting-actions-when-signed-in context answer)
                                                                                               (voting-actions-when-signed-out context answer)))
                                                                     [:.clj-writer-note-item-container] (when (:note answer)
                                                                                                                 (display-writer-note answer)))
                                      [:.clj-jump-to-answer] (when (and (tf/open? objective) user) identity)

                                      [:.clj-answer-new] (when (tf/open? objective) identity)
                                      
                                      [:.clj-answer-form]
                                      (if user
                                        (html/do->
                                         (html/set-attr :action
                                                        (str "/objectives/" (:_id objective)
                                                             "/questions/" (:_id question) "/answers"))
                                         (html/prepend (html/html-snippet (anti-forgery-field))))
                                        (html/substitute (sign-in-to-add-answer context)))

                                      [:.l8n-guidance-heading] (tl8 :question-page/guidance-heading)))))))))
