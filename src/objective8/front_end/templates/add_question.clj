(ns objective8.front-end.templates.add-question
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]
            [objective8.utils :as utils]))

(def add-question-template (html/html-resource "templates/jade/add-question.html" {:parser jsoup/parser}))

(def add-question-form-snippet (html/select add-question-template [:.clj-question-create-form]))

(defn apply-validations [{:keys [doc] :as context} nodes]
  (let [validation-data (get-in doc [:flash :validation])
        validation-report (:report validation-data)
        previous-inputs (:data validation-data)]
    (html/at nodes
             [:.clj-question-length-error] (when (contains? (:question validation-report) :length) identity)
             [:.clj-input-question] (html/content (:question previous-inputs)))))

(defn add-question-form [{:keys [anti-forgery-snippet data] :as context}]
  (let [objective-id (get-in data [:objective :_id])]
    (html/at add-question-form-snippet 
             [:.clj-question-create-form] 
             (html/do-> 
               (html/prepend anti-forgery-snippet)  
               (html/set-attr :action (utils/local-path-for :fe/add-question-form-post :id objective-id))))))

(def sign-in-to-add-question-snippet (html/select pf/library-html-resource [:.clj-to-add-question-please-sign-in]))

(defn sign-in-to-add-question [{:keys [ring-request] :as context}]
  (html/at sign-in-to-add-question-snippet  
           [:.clj-to-add-question-sign-in-link] 
           (html/set-attr :href (str "/sign-in?refer=" (:uri ring-request)))))

(defn add-question [{user :user :as context}]
  (if user
    (add-question-form context)
    (sign-in-to-add-question context)))

(defn add-question-page [{:keys [translations data doc] :as context}]
  (let [objective (:objective data)
        objective-id (:_id objective)]
    (->>
     (html/at add-question-template
              [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
              [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
              [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

              [:.clj-guidance-buttons] nil
              [:.l8n-guidance-heading] (html/content (translations :question-create/guidance-heading))
              [:.clj-objective-navigation-item-objective] (html/set-attr :href (utils/local-path-for :fe/objective :id objective-id))
              [:.clj-objective-title] (html/content (:title objective))
              [:.clj-question-create] (html/content (add-question context)))
     (apply-validations context))))
