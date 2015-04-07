(ns objective8.templates.import-draft
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]
            [objective8.utils :as utils]))

(def import-draft-template (html/html-resource "templates/jade/import-draft.html" {:parser jsoup/parser}))

(defn import-draft-page [{:keys [data doc] :as context}]
  (let [objective-id (:objective-id data)]
    (apply str
           (html/emit*
             (tf/translate context
                           (pf/add-google-analytics
                             (html/at import-draft-template
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr :content (:description doc))
                                      [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                      [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                                      [:.clj-import-draft-preview] (html/content nil)
                                      [:.clj-add-draft-link] (html/set-attr :href (utils/local-path-for :fe/add-draft-get :id objective-id))       
                                      [:.clj-import-draft-form] (html/set-attr :action (utils/local-path-for :fe/import-draft-post :id objective-id)))))))))

