(ns objective8.templates.objective-list
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.templates.page-furniture :as f]))   

(def objective-list-template (html/html-resource "templates/jade/objective-list.html" {:parser jsoup/parser}))

(defn- shorten-content [content]
  (let [content (or content "")
        shortened-content (clojure.string/trim (subs content 0 (min (count content) 100)))]
    (when-not (empty? shortened-content)
      (str shortened-content "..."))))

(defn brief-description [objective]
  (shorten-content (:description objective)))

(defn objective-list-page [{:keys [translations data doc] :as context}]
  (let [objectives (:objectives data)]
    (apply str
           (html/emit*
             (html/at objective-list-template
                      [:title] (html/content (:title doc))
                      [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                      [:.clj-status-bar] (html/substitute (f/status-flash-bar context))

                      [:.clj-guidance-buttons] nil
                      [:.l8n-guidance-heading] (html/content (translations :objectives-guidance/heading))
                      [:.l8n-guidance-text-line-1] (html/content (translations :objectives-guidance/text-line-1))
                      [:.l8n-guidance-text-line-2] (html/content (translations :objectives-guidance/text-line-2))

                      [:.l8n-objective-list-title] (html/content (translations :objective-list/page-title))
                      [:.l8n-create-objective-link] (html/content (translations :objective-list/create-objective-link))
                      [:.l8n-objective-list-subtitle] (html/content (translations :objective-list/subtitle))
                      [:.clj-objective-list-item] (html/clone-for 
                                               [objective objectives]
                                               [:.clj-objective-list-item-link] (html/set-attr "href" (str "/objectives/" (:_id objective)))
                                               [:.clj-objective-list-item-title] (html/content (:title objective))
                                               [:.l8n-drafting-begins] (html/content (translations :objective-list/drafting-begins))
                                               [:.clj-objective-drafting-begins-date] (html/content (:end-date objective))
                                               [:.clj-objective-brief-description] (html/content (brief-description objective))))))))
