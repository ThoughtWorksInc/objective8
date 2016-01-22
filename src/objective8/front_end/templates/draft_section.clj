(ns objective8.front-end.templates.draft-section
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.utils :as utils]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))

(def draft-section-template (html/html-resource "templates/jade/draft-section.html" {:parser jsoup/parser}))

(defn draft-section-page [{:keys [data doc] :as context}]
  (let [section (:section data)
        section-uri (:uri section)
        draft-uri (first (clojure.string/split section-uri #"/sections"))]
    (apply str
           (html/emit*
             (tf/translate context           
                           (-> (html/at draft-section-template
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                                      [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                      [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                                      [:.clj-back-to-draft-link] (html/set-attr :href draft-uri)
                                      [:.clj-draft-section] (html/html-content (:section section))
                                      [:.clj-comment-list] (html/content (pf/comment-list context))       
                                      [:.clj-comment-create] (html/content (pf/comment-create context :section)))
                               pf/add-google-analytics
                               pf/add-custom-favicon))))))
