(ns objective8.templates.add-draft
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.templates.page-furniture :as f]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(def add-draft-template (html/html-resource "templates/jade/add-draft.html" {:parser jsoup/parser}))

(defn add-draft-page [{:keys [translations data doc] :as context}]
  (apply str
         (html/emit*
           (html/at add-draft-template
                    [:title] (html/content (:title doc))
                    [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                    [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                    [:.clj-status-bar] (html/substitute (f/status-flash-bar context))

                    [:.clj-add-draft-preview] (when-let [preview (:preview data)]
                                                (html/html-content preview))

                    [:.clj-add-draft-form] (html/set-attr :action (str "/objectives/" (:objective-id data) "/add-draft"))
                    [:.clj-add-draft-content] (html/do-> 
                                                (html/set-attr :placeholder (translations :add-draft/placeholder))
                                                (html/content (:markdown data))) 
                    [:.l8n-preview-action] (html/content (translations :add-draft/preview))
                    [:.l8n-submit-action] (html/content (translations :add-draft/submit))

                    )
           )))
