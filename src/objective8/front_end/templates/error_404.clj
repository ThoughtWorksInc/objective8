(ns objective8.front-end.templates.error-404
  (:require [net.cgrand.enlive-html :as html]
            [objective8.front-end.templates.page-furniture :as f]
            [objective8.front-end.templates.template-functions :as tf]))

(def error-404-template (html/html-resource "templates/jade/error-404.html"))

(defn error-404-page [{:keys [doc] :as context}]
  (apply str
         (html/emit*
           (tf/translate context
                         (-> (html/at error-404-template
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                                    [:.clj-status-bar] (html/substitute (f/status-flash-bar context)))
                             f/add-google-analytics
                             f/add-custom-favicon)))))
