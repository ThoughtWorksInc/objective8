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

                    [:.l8n-index-welcome] (html/content (translations :index/index-welcome))
                    [:.l8n-index-intro] (html/content (translations :index/index-intro))

                    [:.l8n-create-objective-begin] (html/content (translations :index/create-objective-begin))
                    [:.l8n-create-objective-keyword] (html/content (translations :index/create-objective-keyword))
                    [:.l8n-create-objective-end] (html/content (translations :index/create-objective-end))
                    [:.l8n-collaborate-info] (html/content (translations :index/collaborate-info))
                    [:.l8n-draft-info-begin] (html/content (translations :index/draft-info-begin))
                    [:.l8n-draft-info-keyword] (html/content (translations :index/draft-info-keyword))
                    [:.l8n-draft-info-end] (html/content (translations :index/draft-info-end))

                    [:.l8n-objectives-link] (html/content (translations :index/objectives))
                    [:.l8n-learn-more-link] (html/content (translations :index/learn-more))))))
