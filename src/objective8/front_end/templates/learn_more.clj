(ns objective8.front-end.templates.learn-more
  (:require [net.cgrand.enlive-html :as html]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))

(def learn-more (html/html-resource "templates/jade/learn-more.html"))

(defn learn-more-page [{:keys [doc] :as context}]
  (html/at learn-more
              [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
              [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
              [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))))
