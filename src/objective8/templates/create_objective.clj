(ns objective8.templates.create-objective 
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as f]))

(def create-objective-template (html/html-resource "templates/jade/create-objective.html" {:parser jsoup/parser}))

(defn create-objective-page [{:keys [translations data doc] :as context}]
  (let [objective (:objective data)]
    (apply str
           (html/emit*
             (html/at create-objective-template
                      [:title] (html/content (:title doc))
                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-status-bar] (html/substitute (f/status-flash-bar context))

                      [:.clj-guidance-buttons] nil
                      [:.l8n-guidance-heading] (html/content (translations :create-objective-guidance/heading))
                      [:.l8n-guidance-1] (html/content (translations :create-objective-guidance/item-1))
                      [:.l8n-guidance-2] (html/content (translations :create-objective-guidance/item-2))
                      [:.l8n-guidance-3] (html/content (translations :create-objective-guidance/item-3))
                      [:.l8n-create-objective-title] (html/content (translations :create-objective/page-title))
                      [:.clj-create-objective-form] (html/prepend (html/html-snippet (anti-forgery-field)))
                      [:.l8n-objective-title] (html/content (translations :create-objective/headline))
                      [:.l8n-objective-title-helper] (html/content (translations :create-objective/headline-prompt))
                      [:.l8n-objective-goals-title] (html/content (translations :create-objective/goals))
                      [:.l8n-objective-goals-helper] (html/content (translations :create-objective/goals-prompt))
                      [:.l8n-objective-background] (html/content (translations :create-objective/background))
                      [:.l8n-objective-background-helper] (html/content (translations :create-objective/background-prompt))
                      [:.l8n-create-objective-submit] (html/content (translations :create-objective/submit-text)))))))
