(ns objective8.front-end.templates.error-pages
  (:require [net.cgrand.enlive-html :as html]
            [objective8.front-end.templates.page-furniture :as pf]))

(def error-configuration-template (html/html-resource "templates/jade/error-configuration.html"))

(defn error-configuration-page [{:keys [doc] :as context}]
  (html/at error-configuration-template
           [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
           [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
           [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))))

(def error-default-template (html/html-resource "templates/jade/error-default.html"))

(defn error-default-page [{:keys [doc] :as context}]
  (html/at error-default-template
           [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
           [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
           [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))))
