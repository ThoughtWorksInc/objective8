(ns objective8.front-end.templates.sign-in
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.templates.page-furniture :as f]
            [objective8.config :as config]))

(def sign-in-template (html/html-resource "templates/jade/sign-in.html" {:parser jsoup/parser}))

(defn twitter-credentials-present? []
  (and (get-in config/environment [:twitter-credentials :consumer-token])
       (get-in config/environment [:twitter-credentials :secret-token])))

(defn facebook-credentials-present? []
  (and (get-in config/environment [:facebook-credentials :client-id])
       (get-in config/environment [:facebook-credentials :client-secret])))

(defn use-facebook-login? []
  (and (facebook-credentials-present?) (not (get config/environment :private-mode-enabled))))

(defn use-twitter-login? []
  (and (twitter-credentials-present?) (not (get config/environment :private-mode-enabled))))

(defn stonecutter-credentials-present? []
  (and (:stonecutter-auth-provider-url config/environment)
       (:stonecutter-client-id config/environment)
       (:stonecutter-client-secret config/environment)))

(defn keep-node-if [pred]
  (when (pred)
    identity))

(defn set-helper-text-translation! []
  (cond
    (and (twitter-credentials-present?)
         (facebook-credentials-present?)) (html/set-attr :data-l8n "content:sign-in/twitter-and-facebook-helper-text")
    (facebook-credentials-present?) (html/set-attr :data-l8n "content:sign-in/facebook-helper-text")
    (twitter-credentials-present?) (html/set-attr :data-l8n "content:sign-in/twitter-helper-text")))

(defn sign-in-page [{:keys [doc] :as context}]
  (html/at sign-in-template
           [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
           [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
           [:.clj-status-bar] (html/substitute (f/status-flash-bar context))
           [:.clj-sign-in-twitter] (keep-node-if use-twitter-login?)
           [:.clj-sign-in-facebook] (keep-node-if use-facebook-login?)
           [:.clj-sign-in-d-cent] (keep-node-if stonecutter-credentials-present?)
           [:.helper-text] (set-helper-text-translation!)))
