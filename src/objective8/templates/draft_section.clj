(ns objective8.templates.draft-section 
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.utils :as utils]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf])) 

(def draft-section-template (html/html-resource "templates/jade/draft-section.html" {:parser jsoup/parser}))

(defn draft-section-page [{:keys [data doc] :as context}]
  (let [section (:section data)]
    (apply str
           (html/emit*
             (tf/translate context           
                           (pf/add-google-analytics
                             (html/at draft-section-template 
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                      [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                      [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                                      [:.clj-draft-section] (html/html-content section)
                                      [:.clj-comment-list] (html/content (pf/comment-list context))       
                                      [:.clj-comment-create] (html/content (pf/comment-create context :section)))))))))
