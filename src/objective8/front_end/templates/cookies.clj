(ns objective8.front-end.templates.cookies
  (:require [net.cgrand.enlive-html :as html]
            [objective8.front-end.templates.page-furniture :as pf]))

(def cookie-template (html/html-resource "templates/jade/cookie-page.html"))

(defn cookie-page [{:keys [doc] :as context}]
  (html/at cookie-template
           [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
           [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
           [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))))