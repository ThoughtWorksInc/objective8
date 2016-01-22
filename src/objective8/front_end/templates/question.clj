(ns objective8.front-end.templates.question
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [cemerick.url :as url]
            [objective8.utils :as utils]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]
            [objective8.front-end.config :as fe-config]))

(def question-template (html/html-resource "templates/jade/question.html" {:parser jsoup/parser}))

(defn facebook-meta-tags [{:keys [data ring-request translations] :as context}]
  (let [question (:question data)]
    (html/transformation
     [:.clj-meta-sharing-facebook-title] (html/set-attr :content (:question question))
     [:.clj-meta-sharing-facebook-url] (html/set-attr :content (str utils/host-url (:uri ring-request)))
     [:.clj-meta-sharing-facebook-description] (html/set-attr :content (translations :question-page/facebook-description))
     [:.clj-meta-sharing-facebook-image] nil)))

(defn voting-actions-when-signed-in [{:keys [anti-forgery-snippet ring-request] :as context} answer]
  (html/transformation
    [:.clj-approval-form] (html/prepend anti-forgery-snippet)
    [:.clj-vote-on-uri] (html/set-attr :value (:uri answer))
    [:.clj-refer] (html/set-attr :value (str (:uri ring-request) "?offset=" (get-in context [:data :offset]) "#answer-" (:_id answer)))
    [:.clj-up-score] (html/content (str (get-in answer [:votes :up])))
    [:.clj-down-score] (html/content (str (get-in answer [:votes :down])))))

(defn voting-actions-when-signed-out [{:keys [ring-request] :as context} answer]
  (html/transformation
    [:.clj-approval-form] (html/set-attr :method "get")
    [:.clj-approval-form] (html/set-attr :action "/sign-in")
    [:.clj-vote-on-uri] nil
    [:.clj-refer] (html/set-attr :value (str (:uri ring-request) "?offset=" (get-in context [:data :offset]) "#answer-" (:_id answer)))
    [:.clj-up-score] (html/content (str (get-in answer [:votes :up])))
    [:.clj-down-score] (html/content (str (get-in answer [:votes :down])))))

(defn display-writer-note [answer]
  (html/transformation
    [:.clj-writer-note-item-content] (html/content (:note answer))))

(def sign-in-to-add-answer-snippet (html/select pf/library-html-resource [:.clj-to-add-answer-please-sign-in]))

(defn sign-in-to-add-answer [{:keys [ring-request] :as context}]
  (html/at sign-in-to-add-answer-snippet  
           [:.clj-to-add-answer-sign-in-link] 
           (html/set-attr :href (str "/sign-in?refer=" (:uri ring-request) "?offset=" (get-in context [:data :offset]) "%23add-an-answer"))))

(defn apply-validations [{:keys [doc] :as context} nodes]
  (let [validation-data (get-in doc [:flash :validation])
        validation-report (:report validation-data)
        previous-inputs (:data validation-data)]
    (html/at nodes
             [:.clj-answer-length-error] (when (contains? (:answer validation-report) :length) identity)
             [:.clj-answer-empty-error] (when (contains? (:answer validation-report) :empty) identity)
             [:.clj-input-answer] (html/content (:answer previous-inputs)))))

(def navigation-snippet (html/select question-template [:.clj-secondary-navigation]))

(defn answers-navigation [{:keys [data] :as context}]
  (let [{question-id :_id :as question} (:question data)
        {objective-id :_id :as objective} (:objective data)
        offset (:offset data)
        answers-count (get-in question [:meta :answers-count])
       question-url (url/url (utils/path-for :fe/question :id objective-id :q-id question-id))]
    (html/at navigation-snippet 
             [:.clj-parent-link] (html/set-attr :href (-> (utils/path-for :fe/objective :id objective-id)
                                                          url/url
                                                          (assoc :anchor "questions")))
             [:.clj-parent-text] (html/content (:title objective))

             [:.clj-secondary-navigation-previous] 
             (when (> offset 0) 
               (html/transformation
                 [:.clj-secondary-navigation-previous-link] 
                 (html/set-attr :href
                                (-> question-url
                                    (assoc :query {:offset (- offset fe-config/answers-pagination)})))))

             [:.clj-secondary-navigation-next] 
             (when (> answers-count (+ offset fe-config/answers-pagination))
               (html/transformation
                 [:.clj-secondary-navigation-next-link] 
                 (html/set-attr :href
                                (-> question-url
                                    (assoc :query {:offset (+ offset fe-config/answers-pagination)}))))))))

(defn question-page [{:keys [anti-forgery-snippet translations data user doc] :as context}]
  (let [question (:question data)
        answers (:answers data)
        objective (:objective data)
        offset (:offset data)
        answers-count (get-in question [:meta :answers-count])
        more-answers? (> answers-count (+ offset fe-config/answers-pagination))
        tl8 (tf/translator context)]
    (->> (html/at question-template
                  [:title] (html/content (:title doc))
                  [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                  [:head] (facebook-meta-tags context)
                  [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                  [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

                  [:.clj-secondary-navigation] (html/substitute (answers-navigation context))
                  [:.clj-question] (html/content (:question question))
                  [:.clj-empty-answer-list-item] (when (= answers-count 0) identity)
                  [:.clj-answer] (html/clone-for [answer answers]
                                                 [:.clj-answer] (html/set-attr :id (str "answer-" (:_id answer)))
                                                 [:.clj-answer-text] (html/content (:answer answer))
                                                 [:.clj-approval-form] (if user
                                                                         (voting-actions-when-signed-in context answer)
                                                                         (voting-actions-when-signed-out context answer))
                                                 [:.clj-writer-note-item-container] (when (:note answer)
                                                                                      (display-writer-note answer)))

                  [:.clj-answer-new] (when-not more-answers? identity)

                  [:.clj-answer-form] (if user
                                        (html/do->
                                          (html/set-attr :action
                                                         (str "/objectives/" (:_id objective)
                                                              "/questions/" (:_id question) "/answers"))
                                          (html/prepend anti-forgery-snippet))
                                        (html/substitute (sign-in-to-add-answer context))) 

                  [:.l8n-guidance-heading] (tl8 :question-page/guidance-heading))
         (apply-validations context)
         pf/add-google-analytics
         pf/add-custom-favicon
         (tf/translate context)
         html/emit*
         (apply str))))
