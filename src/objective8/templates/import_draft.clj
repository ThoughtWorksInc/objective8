(ns objective8.templates.import-draft
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]  
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]
            [objective8.utils :as utils]))

(def import-draft-template (html/html-resource "templates/jade/import-draft.html" {:parser jsoup/parser}))

(def submit-button-snippet (html/select pf/library-html-resource [:.clj-form-button]))

(defn submit-button [{:keys [translations] :as context}]
  (html/at submit-button-snippet
           [:.clj-form-button] (html/do-> 
                                 (html/content (translations :import-draft/submit))
                                 (html/set-attr :name "action")
                                 (html/set-attr :value "submit"))))

(defn import-draft-page [{:keys [data doc] :as context}]
  (let [objective-id (:objective-id data)
        import-draft-preview-html (get-in doc [:flash :import-draft-preview-html])]
    (apply str
           (html/emit*
             (tf/translate context
                           (pf/add-google-analytics
                             (html/at import-draft-template
                                      [:title] (html/content (:title doc))
                                      [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr :content (:description doc))
                                      [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                                      [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                                      [:.clj-import-draft-preview] (if import-draft-preview-html
                                                                     (html/html-content import-draft-preview-html)
                                                                     (html/content nil))
                                      [:.clj-add-draft-link] (html/set-attr :href (utils/local-path-for :fe/add-draft-get :id objective-id))       
                                      [:.clj-select-file-link] (when-not import-draft-preview-html
                                                                 identity)
                                      [:.clj-preview-draft-button] (if import-draft-preview-html
                                                                     (html/substitute (submit-button context))
                                                                     identity)
                                      [:.clj-import-draft-form] (html/do-> 
                                                                  (html/prepend (html/html-snippet (anti-forgery-field)))
                                                                  (html/set-attr :action (utils/local-path-for :fe/import-draft-post :id objective-id)))
                                      [:.clj-google-doc-html-content] (html/set-attr :value import-draft-preview-html)

                                      [(html/attr= :src "/static/gapi/google_docs_import.js")] (when-not import-draft-preview-html
                                                                                                 identity)
                                      [:.clj-gapi-js] (when-not import-draft-preview-html
                                                        identity)))))))) 
