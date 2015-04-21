(ns objective8.templates.dashboard-questions

  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :as f]
            [objective8.templates.template-functions :as tf]))


(def dashboard-questions-template (html/html-resource "templates/jade/writer-dashboard.html"))
(def dashboard-questions-navigation-item-snippet (html/select dashboard-questions-template
                                                              [[:.clj-writer-dashboard-navigation-item html/first-of-type]]))

(defn navigation-list-items [{:keys [data] :as context}]
  (let [questions (:questions data)]
    (html/at dashboard-questions-navigation-item-snippet
             [:.clj-writer-dashboard-navigation-item]
             (html/clone-for [question questions]
                             [:.clj-writer-dashboard-navigation-item-label] (html/content (:question question))
                             [:.clj-writer-dashboard-navigation-item-link-count] nil))))

(defn navigation-list [{:keys [data] :as context}]
  (let [questions (:questions data)]
    (if (empty? questions)
      nil
      (navigation-list-items context))))

(defn dashboard-questions [{:keys [doc data] :as context}]
  (let [objective (:objective data)]
    (apply str
           (html/emit*
            (tf/translate context
                          (f/add-google-analytics
                           (html/at dashboard-questions-template
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                                    [:.clj-status-bar] (html/substitute (f/status-flash-bar context))

                                    [:.clj-writer-dashboard-title] (html/content (:title objective))
                                    [:.clj-writer-dashboard-stats] nil
                                    [:.clj-writer-dashboard-navigation-list] (html/content (navigation-list context))
                                    [:.clj-writer-dashboard-filter-list] nil
                                    )))))))
