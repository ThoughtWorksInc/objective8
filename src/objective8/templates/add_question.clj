(ns objective8.templates.add-question
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.templates.page-furniture :as f]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(def objective-write-a-question-template (html/html-resource "templates/jade/objective-write-a-question.html" {:parser jsoup/parser}))

(defn add-question-page [{:keys [user] :as context}]
  (apply str
    (html/emit*
      (html/at objective-write-a-question-template
        [:title] (html/content (get-in context [:doc :title]))
        [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
        [:.clj-status-bar] (html/substitute (f/status-flash-bar context))
        [:.clj-question-create] (if user
                      identity
                      (html/content "!You must sign in to add a question"))

        [:.clj-question-create] (html/prepend
                                  (html/html-snippet (anti-forgery-field)))


        ))))

