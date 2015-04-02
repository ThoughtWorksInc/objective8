(ns objective8.templates.add-question
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]
            [objective8.utils :as utils]))

(def add-question-template (html/html-resource "templates/jade/add-question.html" {:parser jsoup/parser}))

(def add-question-form-snippet (html/select pf/library-html-resource [:.clj-question-create-form]))

(defn add-question-form [{:keys [translations data doc] :as context}]
  (let [objective-id (get-in data [:objective :_id])]
    (html/at add-question-form-snippet 
             [:.clj-question-create-form] 
             (html/do-> 
               (html/prepend (html/html-snippet (anti-forgery-field)))  
               (html/set-attr :action (utils/local-path-for :fe/add-question-form-post :id objective-id))))))

(defn sign-in-to-add-question [{:keys [translations ring-request] :as context}]
  (html/at pf/please-sign-in-snippet
           [:.l8n-before-link] (html/content (translations :question-sign-in/please))
           [:.l8n-sign-in-link] (html/do->
                                  (html/set-attr :href (str "/sign-in?refer=" (:uri ring-request)))
                                  (html/content (translations :question-sign-in/sign-in)))
           [:.l8n-after-link] (html/content (translations :question-sign-in/to))))

(defn add-question [{user :user :as context}]
  (if user
    (add-question-form context)
    (sign-in-to-add-question context)))

(defn add-question-page [{:keys [translations data doc] :as context}]
  (let [objective (:objective data)
        objective-id (:_id objective)]
    (apply str
           (html/emit*
             (tf/translate context
                           (pf/add-google-analytics
                             (html/at add-question-template
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                      [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                      [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

                                      [:.clj-guidance-buttons] nil
                                      [:.l8n-guidance-heading] (html/content (translations :question-create/guidance-heading))
                                      [:.clj-objective-navigation-item-objective] (html/set-attr :href (utils/local-path-for :fe/objective :id objective-id))
                                      [:.clj-objective-title] (html/content (:title objective))
                                      [:.clj-question-create] (html/content (add-question context)))))))))
