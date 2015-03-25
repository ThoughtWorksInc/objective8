(ns objective8.templates.index 
  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :as f])) 

(def index-template (html/html-resource "templates/jade/index.html"))

(defn index-page [{:keys [translations data doc] :as context}]
  (apply str
         (html/emit*
           (html/at index-template
                    [:title] (html/content (:title doc))
                    [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                    [:.clj-status-bar] (html/substitute (f/status-flash-bar context))

                    [:.clj-index-welcome] (html/content (translations :index/index-welcome))
                    [:.clj-index-intro] (html/content (translations :index/index-intro))

                    [:.clj-create-objective-begin] (html/content (translations :index/create-objective-begin))
                    [:.clj-create-objective-keyword] (html/content (translations :index/create-objective-keyword))
                    [:.clj-create-objective-end] (html/content (translations :index/create-objective-end))
                    [:.clj-collaborate-info] (html/content (translations :index/collaborate-info))
                    [:.clj-draft-info-begin] (html/content (translations :index/draft-info-begin))
                    [:.clj-draft-info-keyword] (html/content (translations :index/draft-info-keyword))
                    [:.clj-draft-info-end] (html/content (translations :index/draft-info-end))

                    [:.clj-objectives-link] (html/content (translations :index/objectives))
                    [:.clj-learn-more-link] (html/content (translations :index/learn-more))))))
