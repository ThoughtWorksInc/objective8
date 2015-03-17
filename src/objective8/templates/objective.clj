(ns objective8.templates.objective
  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :as f]))   

(def objective-template (html/html-resource "templates/jade/objective.html"))

(defn objective-page [context]
  (apply str
         (html/emit*
           (html/at objective-template
                    [:title] (html/content (get-in context [:doc :title]))
                    [:.clj-user-navigation-signed-out] (html/substitute (f/user-navigation-signed-in? context))))))
