(ns objective8.templates.dashboard-questions
  (:require [net.cgrand.enlive-html :as html]
            [cemerick.url :as url]
            [objective8.utils :as utils]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))


(def dashboard-questions-template (html/html-resource "templates/jade/questions-dashboard.html"))

(def dashboard-questions-answer-item-snippet (html/select dashboard-questions-template
                                                          [[:.clj-dashboard-answer-item html/first-of-type]]))

(def dashboard-questions-no-answers-snippet (html/select pf/library-html-resource
                                                         [:.clj-dashboard-no-answer-item]))

(def answer-with-no-saved-item-snippet (html/select pf/library-html-resource [:.clj-library-key--dashboard-answer-with-no-saved-item]))

(def answer-with-saved-item-snippet (html/select pf/library-html-resource [:.clj-library-key--dashboard-answer-with-saved-item]))

(defn render-item-without-note [{:keys [ring-request] :as context} answer]
  (html/at answer-with-no-saved-item-snippet
           [:.clj-dashboard-answer-item-text] (html/content (:answer answer)) 
           [:.clj-dashboard-answer-item-up-count] (html/content (str (get-in answer [:votes :up]))) 
           [:.clj-dashboard-answer-item-down-count] (html/content (str (get-in answer [:votes :down]))) 
           [:.clj-refer] (html/set-attr :value (:uri ring-request)) 
           [:.clj-note-on-uri] (html/set-attr :value (:uri answer))
           [:.clj-dashboard-answer-item-save] (html/prepend (html/html-snippet (anti-forgery-field)))
           [:.clj-dashboard-answer-item-save] identity))

(defn render-item-with-note [context answer]
  (html/at answer-with-saved-item-snippet
           [:.clj-dashboard-answer-item-text] (html/content (:answer answer)) 
           [:.clj-dashboard-answer-item-up-count] (html/content (str (get-in answer [:votes :up]))) 
           [:.clj-dashboard-answer-item-down-count] (html/content (str (get-in answer [:votes :down]))) 
           [:.clj-dashboard-answer-item-saved-content] (html/content (:note answer))))

(defn answer-list-items [{:keys [data] :as context}]
  (let [answers (:answers data)]
    (html/at answer-with-no-saved-item-snippet
             [:.clj-dashboard-answer-item]
             (html/clone-for [answer answers]
                             [:.clj-dashboard-answer-item]  (if (contains? answer :note)
                                                             (html/content ( render-item-with-note context answer))
                                                              (html/content (render-item-without-note context answer))
                                                              )))))

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
  (let [objective (:objective data)
        selected-question-uri (:selected-question-uri data)
        dashboard-url (url/url (utils/path-for :fe/dashboard-questions :id (:_id objective)))
        answer-sort-method (:answers-sorted-by data)]
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
                                      [:.clj-dashboard-answer-list] (html/content (answer-list context))
                                      [:.clj-dashboard-filter-paper-clip] nil
                                      [:.clj-dashboard-filter-up-votes] (html/set-attr :href
                                                                                       (str (assoc dashboard-url
                                                                                                   :query {:selected selected-question-uri
                                                                                                           :sorted-by "up-votes"})))

                                      [:.clj-dashboard-filter-up-votes] (if (= answer-sort-method "up-votes")
                                                                          (html/add-class "on")
                                                                          identity)

                                      [:.clj-dashboard-filter-down-votes] (html/set-attr :href
                                                                                         (str (assoc dashboard-url
                                                                                                     :query {:selected selected-question-uri
                                                                                                             :sorted-by "down-votes"})))

                                      [:.clj-dashboard-filter-down-votes] (if (= answer-sort-method "down-votes")
                                                                            (html/add-class "on")
                                                                            identity)

                                      [:.clj-dashboard-content-stats] nil
                                      )))))))
