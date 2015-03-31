(ns objective8.templates.error-404
  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :as f])) 

(def error-404-template (html/html-resource "templates/jade/404.html"))

(defn error-404-page [{:keys [translations doc] :as context}]
  (apply str
         (html/emit*
           (f/add-google-analytics
             (html/at error-404-template
                      [:title] (html/content (:title doc))
                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-status-bar] (html/substitute (f/status-flash-bar context))
                      [:.l8n-error404-page-title] (html/content (translations :error-404/page-title))
                      [:.l8n-error404-page-intro] (html/content (translations :error-404/page-intro))
                      [:.clj-error-404-content] (html/html-content (translations :error-404/page-content)))))))
