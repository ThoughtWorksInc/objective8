(ns objective8.front-end.templates.dashboard-questions
  (:require [net.cgrand.enlive-html :as html]
            [cemerick.url :as url]
            [objective8.utils :as utils]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))


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

(defn apply-writer-note-form-validations [{:keys [doc] :as context} answer nodes]
  (let [validation-data (get-in doc [:flash :validation])
        validation-report (:report validation-data)
        previous-inputs (:data validation-data)
        is-relevant-answer (= (:uri answer)
                              (:note-on-uri previous-inputs))]
    (if is-relevant-answer
      (html/at nodes
               [:.clj-writer-note-empty-error] (when (contains? (:note validation-report) :empty) identity)
               [:.clj-writer-note-length-error] (when (contains? (:note validation-report) :length) identity)
               [:.clj-writer-note-item-field] (html/set-attr :value (:note previous-inputs)))
      (html/at nodes
               [:.clj-writer-note-empty-error] nil    
               [:.clj-writer-note-length-error] nil))))

(defn render-answer-without-note [{:keys [anti-forgery-snippet ring-request] :as context} answer]
  (->> (html/at no-writer-note-snippet
                [:.clj-dashboard-answer-item-text] (html/content (:answer answer)) 
                [:.clj-dashboard-answer-item-up-count] (html/content (str (get-in answer [:votes :up]))) 
                [:.clj-dashboard-answer-item-down-count] (html/content (str (get-in answer [:votes :down]))) 
                [:.clj-refer] (html/set-attr :value (utils/referer-url ring-request))
                [:.clj-note-on-uri] (html/set-attr :value (:uri answer))
                [:.clj-dashboard-writer-note-form] (html/prepend anti-forgery-snippet))
       (apply-writer-note-form-validations context answer)))

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
                             [:.clj-dashboard-navigation-item-link-count] (html/content (str "(" (get-in question [:meta :answers-count]) ")" ))))))

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
                [:.clj-writer-dashboard-navigation-annotations-link] (html/set-attr :href (utils/path-for :fe/dashboard-annotations :id (:_id objective)))
                [:.clj-dashboard-navigation-list] (html/content (navigation-list context))
                [:.clj-dashboard-answer-list] (html/substitute (answer-list context))

                [:.clj-dashboard-filter-paper-clip]
                (html/set-attr :href
                               (str (assoc dashboard-url
                                      :query {:selected    selected-question-uri
                                              :answer-view "paperclip"})))

                [:.clj-dashboard-filter-up-votes]
                (html/set-attr :href
                               (str (assoc dashboard-url
                                      :query {:selected    selected-question-uri
                                              :answer-view "up-votes"})))

                [:.clj-dashboard-filter-down-votes]
                (html/set-attr :href
                               (str (assoc dashboard-url
                                      :query {:selected    selected-question-uri
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

                [:.clj-dashboard-content-stats] nil)))
