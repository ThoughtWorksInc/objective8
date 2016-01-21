(ns objective8.front-end.templates.create-objective
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))

(def create-objective-template (html/html-resource "templates/jade/create-objective.html" {:parser jsoup/parser}))

(defn apply-validations [{:keys [doc] :as context} nodes]
  (let [validation-data (get-in doc [:flash :validation])
        validation-report (:report validation-data)
        previous-inputs (:data validation-data)]
    (html/at nodes
             [:.clj-title-length-error] (when (contains? (:title validation-report) :length) identity)
             [:.clj-input-objective-title] (html/set-attr :value (:title previous-inputs))

             [:.clj-description-empty-error] (when (contains? (:description validation-report) :empty) identity)
             [:.clj-description-length-error] (when (contains? (:description validation-report) :length) identity)
             [:.clj-input-objective-background] (html/content (:description previous-inputs)))))

(defn create-objective-page [{:keys [anti-forgery-snippet doc] :as context}]
  (->>
   (html/at create-objective-template
            [:title] (html/content (:title doc))
            [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
            [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
            [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

            [:.clj-guidance-buttons] nil
            [:.clj-create-objective-form] (html/prepend anti-forgery-snippet))
   (apply-validations context)
   pf/add-google-analytics
   pf/add-custom-favicon
   (tf/translate context)
   html/emit*
   (apply str)))
