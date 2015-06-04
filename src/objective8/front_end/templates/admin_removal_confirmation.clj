(ns objective8.front-end.templates.admin-removal-confirmation 
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))

(def admin-removal-confirmation-template (html/html-resource "templates/jade/admin-removal-confirmation.html" {:parser jsoup/parser}))

(defn admin-removal-confirmation-page [{:keys [anti-forgery-snippet doc data] :as context}]
  (let [{:keys [removal-uri removal-sample] :as removal-data} (:removal-data data)]
    (apply str
           (html/emit*
             (tf/translate context
                           (pf/add-google-analytics
                             (html/at admin-removal-confirmation-template 
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr :content (:description doc))
                                      [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                                      [:.clj-objective-title] (html/content removal-sample)
                                      [:.clj-removal-confirmation-form] (html/prepend anti-forgery-snippet)
                                      [:.clj-removal-uri] (html/set-attr :value removal-uri))))))))
