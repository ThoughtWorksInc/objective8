(ns objective8.front-end.templates.import-draft
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]
            [objective8.utils :as utils]))

(def import-draft-template (html/html-resource "templates/jade/import-draft.html" {:parser jsoup/parser}))

(def submit-button-snippet (html/select pf/library-html-resource [:.clj-form-button]))

(defn submit-button [{:keys [translations] :as context}]
  (html/at submit-button-snippet
           [:.clj-form-button] (html/do-> 
                                 (html/content (translations :import-draft/submit))
                                 (html/set-attr :name "action")
                                 (html/set-attr :value "submit"))))

(defn apply-validations [{:keys [doc] :as context} nodes]
  (let [validation-data (get-in doc [:flash :validation])
        validation-report (:report validation-data)
        previous-inputs (:data validation-data)]
    (html/at nodes
             [:.clj-draft-content-empty-error] (when (contains? (:content validation-report) :empty) identity))))

(defn import-draft-page [{:keys [anti-forgery-snippet data doc] :as context}]
  (let [objective-id (:objective-id data)
        import-draft-preview-html (get-in doc [:flash :import-draft-preview-html])]
    (->>
      (html/at import-draft-template
               [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr :content (:description doc))
               [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
               [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
               [:.clj-import-draft-preview] (when import-draft-preview-html
                                              (html/html-content import-draft-preview-html))
               [:.clj-cancel-link] (html/set-attr :href (utils/local-path-for :fe/draft-list :id objective-id))       
               [:.clj-select-file-link] (when-not import-draft-preview-html
                                          identity)
               [:.clj-preview-draft-button] (if import-draft-preview-html
                                              (html/substitute (submit-button context))
                                              identity)
               [:.clj-import-draft-form] (html/do-> 
                                           (html/prepend anti-forgery-snippet)
                                           (html/set-attr :action (utils/local-path-for :fe/import-draft-post :id objective-id)))
               [:.clj-google-doc-html-content] (html/set-attr :value import-draft-preview-html)

               [(html/attr= :src "/static/gapi/google_docs_import.js")] (when-not import-draft-preview-html
                                                                          identity)
               [:.clj-gapi-js] (when-not import-draft-preview-html
                                 identity))
      (apply-validations context))))
