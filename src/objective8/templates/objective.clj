(ns objective8.templates.objective
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.templates.page-furniture :as f]))   

(def objective-template (html/html-resource "templates/jade/objective.html" {:parser jsoup/parser}))

(defn objective-page [{:keys [translations data] :as context}]
  (let [title (get-in data [:objective :title])
        description (get-in data [:objective :description])
        candidates (:candidates data)]
    (apply str
           (html/emit*
             (html/at objective-template
                      [:title] (html/content (get-in context [:doc :title]))
                      [:.clj-user-navigation-signed-out] (html/substitute (f/user-navigation-signed-in? context))
                      [:.clj-objective-title] (html/content title)
                      [:.clj-replace-with-objective-detail] (html/substitute (f/text->p-nodes description))
                      [:.clj-writers-section-title] (html/content (translations :objective-view/writers))
                      [:.clj-writer-item-list] (html/content (f/writer-list context)) 
                      )))))
