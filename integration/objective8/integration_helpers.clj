(ns objective8.integration-helpers
  (:require [net.cgrand.enlive-html :as html]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.core :as core]))

(defn get-anti-forgery-token [request-context]
  (let [raw-html (:body (:response request-context))]
   (do
    (get-in (first (html/select (html/html-snippet raw-html) [:#__anti-forgery-token])) [:attrs :value]))))


(defn with-sign-in [user-session url & args]
  (get-anti-forgery-token user-session)
  (let [request-function #(apply p/request % url args)]
    (-> user-session
        ; Hit unauthorized url
        request-function
        ; Retrieve twitter user-name (NB: requires twitter authentication background to be set)
        (p/request "http://localhost:8080/twitter-callback?oauth_verifier=the-verifier")
        ; Post user email address to store --- returns authentication map
        (p/request "http://localhost:8080/sign-up" :request-method :post)
        ; Follow redirect to originally requested resource
        (p/follow-redirect))))

(defn test-context
  "Creates a fake application context which uses the provided atom
   as database storage"

  ([]
   (p/session (core/app core/app-config)))

  ([test-store]
   (p/session (core/app (assoc core/app-config :store test-store)))))

;; Checkers for peridot responses
(defn peridot-response-json-body->map [peridot-response]
  (-> (get-in peridot-response [:response :body])
      (json/parse-string true)))

(defn check-json-body [expected-json-as-map]
  (fn [peridot-response]
    (let [parsed-body (peridot-response-json-body->map peridot-response)]
      (= parsed-body expected-json-as-map))))
