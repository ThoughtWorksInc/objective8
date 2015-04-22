(ns objective8.templates.draft-diff
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf])) 

(def draft-diff-template (html/html-resource "templates/jade/draft-diff.html" {:parser jsoup/parser}))

(defn draft-diff-page [{:keys [data doc] :as context}]
  (apply str
         (html/emit*
           (tf/translate context
                         (pf/add-google-analytics
                           (html/at draft-diff-template 
                                    [:title] (html/content (:title doc))
                                    [:.clj-add-previous-draft] (html/html-content (:previous-draft-diffs data))
                                    [:.clj-add-current-draft] (html/html-content (:current-draft-diffs data))))))))
