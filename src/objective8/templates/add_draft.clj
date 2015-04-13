(ns objective8.templates.add-draft
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]
            [objective8.utils :as utils]))

(def add-draft-template (html/html-resource "templates/jade/add-draft.html" {:parser jsoup/parser}))

(defn add-draft-page [{:keys [data doc] :as context}]
  (let [objective-id (:objective-id data)]
  (apply str
         (html/emit*
           (tf/translate context
                         (pf/add-google-analytics
                           (html/at add-draft-template
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr :content (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                    [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

                                    [:.clj-add-draft-preview] (when-let [preview (:preview data)]
                                                                (html/html-content preview))

                                    [:.clj-add-draft-form] (html/do-> 
                                                             (html/set-attr :action (utils/local-path-for :fe/add-draft-get :id objective-id))
                                                             (html/prepend (html/html-snippet (anti-forgery-field)))) 
                                    [:.clj-add-draft-content] (html/content (:markdown data))       
                                    [:.clj-cancel-link] (html/set-attr :href (utils/local-path-for :fe/draft-list :id objective-id)))))))))
