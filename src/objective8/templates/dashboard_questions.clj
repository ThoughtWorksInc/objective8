(ns objective8.templates.dashboard-questions
  (:require [net.cgrand.enlive-html :as html]
            [cemerick.url :as url]
            [objective8.utils :as utils]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))


(def dashboard-questions-template (html/html-resource "templates/jade/questions-dashboard.html"))

(def dashboard-questions-no-answers-snippet (html/select pf/library-html-resource
                                                         [:.clj-library-key--dashboard-no-answer-item]))

(def answer-with-no-writer-note-snippet (html/select pf/library-html-resource [:.clj-library-key--dashboard-answer-without-writer-note]))

(def answer-with-writer-note-snippet (html/select pf/library-html-resource [:.clj-library-key--dashboard-answer-with-writer-note]))

(def no-writer-note-snippet (html/select answer-with-no-writer-note-snippet [:.clj-dashboard-answer-item]))
(def writer-note-snippet (html/select answer-with-writer-note-snippet [:.clj-dashboard-answer-item]))

(defn dashboard-questions-no-answers [{:keys [translations data] :as context}]
  (let [translation-key (case (:answer-view-type data)
                          :paperclip :writer-dashboard/no-answers-with-writer-notes-message
                          :writer-dashboard/no-answers-message)]
    (html/at dashboard-questions-no-answers-snippet
             [:.clj-dashboard-no-answer-item] (html/content (translations translation-key)))))

(defn render-answer-without-note [{:keys [ring-request] :as context} answer]
  (html/at no-writer-note-snippet 
           [:.clj-dashboard-answer-item-text] (html/content (:answer answer)) 
           [:.clj-dashboard-answer-item-up-count] (html/content (str (get-in answer [:votes :up]))) 
           [:.clj-dashboard-answer-item-down-count] (html/content (str (get-in answer [:votes :down]))) 
           [:.clj-refer] (html/set-attr :value (utils/referer-url ring-request))
           [:.clj-note-on-uri] (html/set-attr :value (:uri answer))
           [:.clj-dashboard-writer-note-form] (html/prepend (html/html-snippet (anti-forgery-field)))))

(defn render-answer-with-note [context answer]
  (html/at writer-note-snippet
           [:.clj-dashboard-answer-item-text] (html/content (:answer answer)) 
           [:.clj-dashboard-answer-item-up-count] (html/content (str (get-in answer [:votes :up]))) 
           [:.clj-dashboard-answer-item-down-count] (html/content (str (get-in answer [:votes :down]))) 
           [:.clj-dashboard-writer-note-text] (html/content (:note answer))))

(defn answer-list-items [{:keys [data] :as context}]
  (let [answers (:answers data)]
    (html/at answer-with-no-writer-note-snippet
             [:.clj-dashboard-answer-item]
             (html/clone-for [answer answers]
                             [:.clj-dashboard-answer-item]  (if (:note answer)
                                                              (html/substitute (render-answer-with-note context answer))
                                                              (html/substitute (render-answer-without-note context answer)))))))

(defn answer-list [{:keys [data] :as context}]
  (let [answers (:answers data)]
    (if (empty? answers)
      (dashboard-questions-no-answers context)
      (answer-list-items context))))

(def dashboard-questions-navigation-item-snippet (html/select dashboard-questions-template
                                                              [[:.clj-dashboard-navigation-item html/first-of-type]]))

(def dashboard-questions-no-questions-snippet (html/select pf/library-html-resource
                                                           [:.clj-dashboard-no-question-item]))

(defn navigation-list-items [{:keys [data ring-request] :as context}]
  (let [questions (:questions data)
        objective (:objective data)
        selected-question-uri (:selected-question-uri data)
        dashboard-url (url/url (utils/path-for :fe/dashboard-questions :id (:_id objective)))]
    (html/at dashboard-questions-navigation-item-snippet
             [:.clj-dashboard-navigation-item]
             (html/clone-for [question questions]
                             [:.clj-dashboard-navigation-item] (if (= selected-question-uri (:uri question))
                                                                 (html/add-class "on")
                                                                 identity)
                             [:.clj-dashboard-navigation-item-label] (html/content (:question question))
                             [:.clj-dashboard-navigation-item-link]
                             (html/set-attr :href
                                            (str (assoc dashboard-url
                                                        :query {:selected (:uri question)}
                                                        :anchor "dashboard-content")))
                             [:.clj-dashboard-navigation-item-link-count] (html/content (str "(" (:answer-count question) ")" ))))))

(defn navigation-list [{:keys [data] :as context}]
  (let [questions (:questions data)]
    (if (empty? questions)
      dashboard-questions-no-questions-snippet
      (navigation-list-items context))))

(defn dashboard-questions [{:keys [doc data] :as context}]
  (let [objective (:objective data)
        selected-question-uri (:selected-question-uri data)
        dashboard-url (url/url (utils/path-for :fe/dashboard-questions :id (:_id objective)))
        answer-view-type (:answer-view-type data)]
    (apply str
           (html/emit*
             (tf/translate context
                           (pf/add-google-analytics
                             (html/at dashboard-questions-template
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                      [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                      [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

                                      [:.clj-dashboard-title-link] (html/set-attr :href (url/url (utils/path-for :fe/objective :id (:_id objective))))
                                      [:.clj-dashboard-title-link] (html/content (:title objective))

                                      [:.clj-dashboard-stat-participant] nil
                                      [:.clj-dashboard-stat-starred-amount] (html/content (str (get-in objective [:meta :stars-count])))
                                      [:.clj-writer-dashboard-navigation-questions-link] (html/set-attr :href (utils/path-for :fe/dashboard-questions :id (:_id objective)))
                                      [:.clj-writer-dashboard-navigation-comments-link] (html/set-attr :href (utils/path-for :fe/dashboard-comments :id (:_id objective)))
                                      [:.clj-dashboard-navigation-list] (html/content (navigation-list context))
                                      [:.clj-dashboard-answer-list] (html/substitute (answer-list context))

                                      [:.clj-dashboard-filter-paper-clip]
                                      (html/set-attr :href
                                                     (str (assoc dashboard-url
                                                                 :query {:selected selected-question-uri
                                                                         :answer-view "paperclip"})))
                                      
                                      [:.clj-dashboard-filter-up-votes]
                                      (html/set-attr :href
                                                     (str (assoc dashboard-url
                                                                 :query {:selected selected-question-uri
                                                                         :answer-view "up-votes"})))

                                      [:.clj-dashboard-filter-down-votes]
                                      (html/set-attr :href
                                                     (str (assoc dashboard-url
                                                                 :query {:selected selected-question-uri
                                                                         :answer-view "down-votes"})))

                                      [:.clj-dashboard-filter-paper-clip] (if (= answer-view-type :paperclip)
                                                                            (html/add-class "on")
                                                                            identity)


                                      [:.clj-dashboard-filter-up-votes] (if (= answer-view-type :up-votes)
                                                                          (html/add-class "on")
                                                                          identity)

                                      [:.clj-dashboard-filter-down-votes] (if (= answer-view-type :down-votes)
                                                                            (html/add-class "on")
                                                                            identity)

                                      [:.clj-dashboard-content-stats] nil)))))))
