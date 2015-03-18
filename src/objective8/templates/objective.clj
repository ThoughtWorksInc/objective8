(ns objective8.templates.objective
  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :as f]))   

(defn text->p-nodes
  "Turns text into a collection of paragraph nodes based on linebreaks.
   Returns nil if no text is supplied"
  [text]
  (when text
    (let [newline-followed-by-optional-whitespace #"(\n+|\r+)\s*"]
    (map (fn [p] (html/html [:p p])) (clojure.string/split text
                                                           newline-followed-by-optional-whitespace)))))

(def objective-template (html/html-resource "templates/jade/objective.html"))

(defn objective-page [context]
  (apply str
         (html/emit*
           (html/at objective-template
                    [:title] (html/content (get-in context [:doc :title]))
                    [:.clj-user-navigation-signed-out] (html/substitute (f/user-navigation-signed-in? context))
                    [:.clj-objective-title] (html/content (get-in context [:data :objective :title]))
                    [:.clj-replace-with-objective-detail] (html/substitute (text->p-nodes (get-in context [:data :objective :description])))))))
