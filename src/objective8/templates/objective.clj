(ns objective8.templates.objective
  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :as f]))   

(def objective-template (html/html-resource "templates/jade/objective.html"))

(defn objective-page [context]
  (let [user (:user context)]
    (apply str
           (html/emit*
             (html/at objective-template
                      [:.clj-user-navigation-signed-out] (if user
                                                           (html/substitute (f/user-navigation-signed-in context))
                                                           (html/substitute (f/user-navigation-signed-out context))))))))
