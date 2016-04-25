(ns objective8.front-end.templates.sign-up
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.templates.page-furniture :as f]))

(def authorisation-template (html/html-resource "templates/jade/authorisation-page.html"))

(defn authorisation-page [{:keys [doc] :as context}]
  (html/at authorisation-template
           [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
           [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
           [:.clj-status-bar] (html/substitute (f/status-flash-bar context))))

(def sign-up-template (html/html-resource "templates/jade/sign-up.html" {:parser jsoup/parser}))

(defn apply-validations [{:keys [doc] :as context} nodes]
  (let [validation-data (get-in doc [:flash :validation])
        validation-report (:report validation-data)
        previous-inputs (:data validation-data)]
    (html/at nodes
             [:.clj-auth-email-invalid-error] (when (= validation-data :auth-email) identity)

             [:.clj-username-invalid-error] (when (contains? (:username validation-report) :invalid) identity)
             [:.clj-username-duplicated-error] (when (contains? (:username validation-report) :duplicated) identity)
             [:.clj-input-username] (html/set-attr :value (:username previous-inputs))

             [:.clj-email-empty-error] (when (contains? (:email-address validation-report) :empty) identity)
             [:.clj-email-invalid-error] (when (contains? (:email-address validation-report) :invalid) identity)
             [:.clj-input-email-address] (html/set-attr :value (:email-address previous-inputs)))))

(defn sign-up-page [{:keys [anti-forgery-snippet translations doc user] :as context}]
  (->> (html/at sign-up-template
                [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
                [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
                [:.clj-status-bar] (html/substitute (f/status-flash-bar context))

                [:.clj-form-email-address] (when-not (:email user)
                                             identity)
                [:.clj-sign-up-form] (html/prepend anti-forgery-snippet)
                [:.clj-username-error] (when-let [error-type (get-in doc [:errors :username])]
                                         (html/content (translations (keyword "sign-up" (name error-type))))))
       (apply-validations context)))
