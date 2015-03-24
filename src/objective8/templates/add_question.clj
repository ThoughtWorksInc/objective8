(ns objective8.templates.add-question
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.templates.page-furniture :as f]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(def objective-write-a-question-template (html/html-resource "templates/jade/objective-write-a-question.html" {:parser jsoup/parser}))

(defn add-question-page [{:keys [translations data] :as context}]
  (let [
        objective (:objective data)]
  (apply str
    (html/emit*
      (html/at objective-write-a-question-template
        [:title] (html/content (get-in context [:doc :title]))
        [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
        [:.clj-status-bar] (html/substitute (f/status-flash-bar context))
        [:.clj-guidance-buttons] nil
        [:.clj-guidance-heading](html/content (translations :question-create/guidance-heading))
        [:.clj-guidance-text](html/content (translations :question-create/guidance-text))
        [:.clj-objective-navigation-item-objective] (html/set-attr "href" (str "/objectives/" (:_id objective)))
        [:.l8n-back-to-objective] (html/content (translations :objective-nav/back-to-objective))
        [:.clj-objective-title] (html/content (:title objective))
        [:.clj-add-question-title] (html/content (translations :question-create/add-a-question))
        [:.clj-question-create-form] (html/content (f/add-question context)))))))

