(ns objective8.front-end.templates.invite-writer
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))

(def invite-writer-template (html/html-resource "templates/jade/invite-writer.html" {:parser jsoup/parser}))

(def invite-writer-form-snippet (html/select pf/library-html-resource [:.clj-invite-a-writer-form]))

(defn invite-writer-form [{:keys [anti-forgery-snippet] :as context}]
  (html/at invite-writer-form-snippet
           [:.clj-invite-a-writer-form] (html/prepend anti-forgery-snippet)))

(def sign-in-to-invite-writer-snippet (html/select pf/library-html-resource [:.clj-to-invite-writer-please-sign-in]))

(defn sign-in-to-invite-writer [{:keys [ring-request] :as context}]
  (html/at sign-in-to-invite-writer-snippet 
           [:.clj-to-invite-writer-sign-in-link] 
           (html/set-attr :href (str "/sign-in?refer=" (:uri ring-request)))))

(defn invite-writer [{user :user :as context}]
  (if user
    (invite-writer-form context) 
    (sign-in-to-invite-writer context)))

(defn apply-validations [{:keys [doc] :as context} nodes]
  (let [validation-data (get-in doc [:flash :validation])
        validation-report (:report validation-data)
        previous-inputs (:data validation-data)]
    (html/at nodes
             [:.clj-writer-name-empty-error] (when (contains? (:writer-name validation-report) :empty) identity)
             [:.clj-writer-name-length-error] (when (contains? (:writer-name validation-report) :length) identity)
             [:.clj-writer-email-empty-error] (when (contains? (:writer-email validation-report) :empty) identity)
             [:.clj-writer-email-invalid-error] (when (contains? (:writer-email validation-report) :invalid) identity)
             [:.clj-writer-reason-empty-error] (when (contains? (:reason validation-report) :empty) identity)
             [:.clj-writer-reason-length-error] (when (contains? (:reason validation-report) :length) identity)

             [:.clj-writer-name-field] (html/set-attr :value (:writer-name previous-inputs))
             [:.clj-writer-email-field] (html/set-attr :value (:writer-email previous-inputs))
             [:.clj-invitation-reason-field] (html/content (:reason previous-inputs)))))

(defn invite-writer-page [{:keys [data doc] :as context}]
  (let [objective (:objective data)]
    (->> (html/at invite-writer-template
                  [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                  [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
                  [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
                  
                  [:.clj-objective-navigation-item-objective] (html/set-attr :href (str "/objectives/" (:_id objective)))
                  [:.clj-objective-title] (html/content (:title objective))
                  
                  [:.clj-invite-a-writer-form] (html/content (invite-writer context))
                  [:.clj-invite-a-writer-form] (html/set-attr :action (str "/objectives/" (:_id objective) "/writer-invitations")))
         (apply-validations context))))
