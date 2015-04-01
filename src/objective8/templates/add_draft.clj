(ns objective8.templates.add-draft
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as f]
            [objective8.templates.template-functions :as tf]))

(def add-draft-template (html/html-resource "templates/jade/add-draft.html" {:parser jsoup/parser}))

(defn add-draft-page [{:keys [data doc] :as context}]
  (apply str
         (html/emit*
           (tf/translate context
                         (f/add-google-analytics
                           (html/at add-draft-template
                                    [:title] (html/content (:title doc))
                                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                    [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                                    [:.clj-status-bar] (html/substitute (f/status-flash-bar context))

                                    [:.clj-add-draft-preview] (when-let [preview (:preview data)]
                                                                (html/html-content preview))

                                    [:.clj-add-draft-form] (html/do-> 
                                                             (html/set-attr :action (str "/objectives/" (:objective-id data) "/add-draft")) 
                                                             (html/prepend (html/html-snippet (anti-forgery-field)))) 
                                    [:.clj-add-draft-content] (html/content (:markdown data))
                                    ))))))
