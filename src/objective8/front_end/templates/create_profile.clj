(ns objective8.front-end.templates.create-profile
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))

(def create-profile-template (html/html-resource "templates/jade/create-profile.html" {:parser jsoup/parser}))

(defn apply-validations [{:keys [doc] :as context} nodes]
  (let [validation-data (get-in doc [:flash :validation])
        validation-report (:report validation-data)
        previous-inputs (:data validation-data)]
    (html/at nodes
             [:.clj-name-length-error] (when (contains? (:name validation-report) :length) identity)
             [:.clj-name-empty-error] (when (contains? (:name validation-report) :empty) identity)
             [:.clj-create-profile-name] (if-let [input-name (:name previous-inputs)] 
                                         (html/set-attr :value input-name)
                                         identity)    
             [:.clj-biog-length-error] (when (contains? (:biog validation-report) :length) identity)
             [:.clj-biog-empty-error] (when (contains? (:biog validation-report) :empty) identity)
             [:.clj-create-profile-biog] (if-let [input-biog (:biog previous-inputs)]
                                         (html/content input-biog)    
                                         identity))))

(defn create-profile-page [{:keys [anti-forgery-snippet doc] :as context}]
  (->> (html/at create-profile-template
                [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                [:.clj-create-profile-form] (html/prepend anti-forgery-snippet))
       (apply-validations context)))
