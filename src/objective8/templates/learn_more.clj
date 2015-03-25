(ns objective8.templates.learn-more
  (:require [net.cgrand.enlive-html :as html]
            [objective8.templates.page-furniture :as f])) 

(def learn-more (html/html-resource "templates/jade/learn-more.html"))

(defn learn-more-page [{:keys [translations doc] :as context}]
  (apply str
         (html/emit*
           (html/at learn-more
                    [:title] (html/content (:title doc))
                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                    [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                    [:.clj-status-bar] (html/substitute (f/status-flash-bar context))
                    [:.clj-learn-more-heading] (html/content (translations :learn-more/page-title))
                    [:.clj-learn-more-sub-heading] (html/content (translations :learn-more/sub-title))
                    [:.clj-learn-more-lede] (html/content (translations :learn-more/page-intro))
                    [:.clj-learn-more-detail] (html/html-content (translations :learn-more/page-content))
                    [:.clj-get-started] (html/do-> 
                                          (html/set-attr "title" (translations :learn-more/get-started-button-title))
                                          (html/content (translations :learn-more/get-started-button-text)))))))
