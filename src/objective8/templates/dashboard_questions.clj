(ns objective8.templates.dashboard-questions
  (:require [net.cgrand.enlive-html :as html]
            [cemerick.url :as url]
            [objective8.utils :as utils]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))


(def dashboard-questions-template (html/html-resource "templates/jade/writer-dashboard.html"))

(def dashboard-questions-answer-item-snippet (html/select dashboard-questions-template
                                                          [[:.clj-dashboard-answer-item html/first-of-type]]))

(def dashboard-questions-no-answers-snippet (html/select pf/library-html-resource
                                                         [:.clj-dashboard-no-answer-item]))

(defn answer-list-items [{:keys [data] :as context}]
  (let [answers (:answers data)]
    (html/at dashboard-questions-answer-item-snippet
             [:.clj-dashboard-answer-item]
             (html/clone-for [answer answers]
                             [:.clj-dashboard-answer-item-text] (html/content (:answer answer))
                             [:.clj-dashboard-answer-item-up-count] (html/content (str (get-in answer [:votes :up])))
                             [:.clj-dashboard-answer-item-down-count] (html/content (str (get-in answer [:votes :down])))))))

(defn answer-list [{:keys [data] :as context}]
  (let [answers (:answers data)]
    (if (empty? answers)
      dashboard-questions-no-answers-snippet
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
  (let [objective (:objective data)]
    (apply str
           (html/emit*
            (tf/translate context
                          (pf/add-google-analytics
                           (html/at dashboard-questions-template
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                    [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

                                    [:.clj-dashboard-title] (html/content (:title objective))
                                    [:.clj-dashboard-stat-participant] nil
                                    [:.clj-dashboard-stat-starred-amount] (html/content (str (get-in objective [:meta :stars-count])))
                                    [:.clj-dashboard-navigation-list] (html/content (navigation-list context))
                                    [:.clj-dashboard-answer-list] (html/content (answer-list context))
                                    [:.clj-dashboard-content-stats] nil
                                    [:.clj-dashboard-filter-list :li] nil
                                    [:.clj-dashboard-answer-item-save] nil)))))))
