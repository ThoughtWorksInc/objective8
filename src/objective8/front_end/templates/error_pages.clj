(ns objective8.front-end.templates.error-pages
  (:require [net.cgrand.enlive-html :as html]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.config :as config]))

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

(def error-log-in-template (html/html-resource "templates/jade/error-log-in.html"))

(defn error-log-in-page [{:keys [doc] :as context}]
  (if (get-in config/environment [:okta-credentials :client-id])
    (html/at error-log-in-template
             [:body] (html/substitute (html/select error-log-in-template [:.app-wrapper]))
             [:header] nil
             [:footer] nil)
    (html/at error-log-in-template
             [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
             [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
             [:.clj-status-bar] (html/substitute (pf/status-flash-bar context)))))