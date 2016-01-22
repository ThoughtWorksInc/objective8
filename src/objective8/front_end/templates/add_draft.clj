(ns objective8.front-end.templates.add-draft
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]
            [objective8.utils :as utils]))

(def add-draft-template (html/html-resource "templates/jade/add-draft.html" {:parser jsoup/parser}))

(defn apply-validations [{:keys [doc] :as context} nodes]
  (let [validation-data (get-in doc [:flash :validation])
        validation-report (:report validation-data)
        previous-inputs (:data validation-data)]
    (html/at nodes
             [:.clj-draft-empty-error] (when (contains? (:markdown validation-report) :empty) identity))))

(defn add-draft-page [{:keys [data doc anti-forgery-snippet] :as context}]
  (let [objective-id (:objective-id data)]
    (->> (html/at add-draft-template
                  [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr :content (:description doc))
                  [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                  [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                  
                  [:.clj-add-draft-preview] (when-let [preview (:preview data)]
                                              (html/html-content preview))
                  
                  [:.clj-add-draft-form] (html/do->
                                          (html/set-attr :action (utils/local-path-for :fe/add-draft-get :id objective-id))
                                          (html/prepend anti-forgery-snippet))
                  [:.clj-add-draft-content] (html/content (:markdown data))
                  [:.clj-cancel-link] (html/set-attr :href (utils/local-path-for :fe/draft-list :id objective-id)))
         (apply-validations context))))
