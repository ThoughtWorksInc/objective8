(ns objective8.front-end.templates.edit-profile
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))

(def edit-profile-template (html/html-resource "templates/jade/edit-profile.html" {:parser jsoup/parser}))

(defn apply-validations [{:keys [doc] :as context} nodes]
  (let [validation-data (get-in doc [:flash :validation])
        validation-report (:report validation-data)
        previous-inputs (:data validation-data)]
    (html/at nodes
             [:.clj-name-length-error] (when (contains? (:name validation-report) :length) identity)
             [:.clj-name-empty-error] (when (contains? (:name validation-report) :empty) identity)
             [:.clj-edit-profile-name] (if-let [input-name (:name previous-inputs)] 
                                         (html/set-attr :value input-name)
                                         identity)    
             [:.clj-biog-length-error] (when (contains? (:biog validation-report) :length) identity)
             [:.clj-biog-empty-error] (when (contains? (:biog validation-report) :empty) identity)
             [:.clj-edit-profile-biog] (if-let [input-biog (:biog previous-inputs)]
                                         (html/content input-biog)    
                                         identity))))

(defn edit-profile-page [{:keys [doc data] :as context}]
  (let [user-profile (:user-profile data)]
    (->>
      (html/at edit-profile-template
               [:title] (html/content (:title doc))
               [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
               [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
               [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
               [:.clj-edit-profile-form] (html/prepend (html/html-snippet (anti-forgery-field)))
               [:.clj-edit-profile-name] (html/set-attr :value (:name user-profile))
               [:.clj-edit-profile-biog] (html/content (:biog user-profile)))
      (apply-validations context)
      pf/add-google-analytics 
      (tf/translate context) 
      html/emit*
      (apply str))))
