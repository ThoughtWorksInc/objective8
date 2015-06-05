(ns objective8.front-end.templates.error-pages
  (:require [net.cgrand.enlive-html :as html]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf])) 

(defn render-hiccup [context hiccup-template]
  (->> (hiccup-template context)
       pf/add-google-analytics
       (tf/translate context)
       html/emit*
       (apply str)))

(def error-configuration-template (html/html-resource "templates/jade/error-configuration.html"))

(defn error-configuration-hiccup [{:keys [doc] :as context}]
  (html/at error-configuration-template
           [:title] (html/content (:title doc))
           [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
           [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
           [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))))

(defn error-configuration-page [context]
  (render-hiccup context error-configuration-hiccup))

(def error-default-template (html/html-resource "templates/jade/error-default.html"))

(defn error-default-hiccup [{:keys [doc] :as context}]
  (html/at error-default-template
           [:title] (html/content (:title doc))
           [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
           [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
           [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))))

(defn error-default-page [context]
  (render-hiccup context error-default-hiccup))
