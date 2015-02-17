(ns objective8.integration-helpers
  (:require [net.cgrand.enlive-html :as html]
            [midje.sweet :as midje]
            [korma.core :as korma]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.core :as core]
            [objective8.storage.mappings :as m]
            [objective8.storage.database :as db]
            [midje.sweet :as midje]))

(defn db-connection [] (db/connect! db/postgres-spec))

(defn truncate-tables []
  (korma/delete m/bearer-token)
  (korma/delete m/answer)
  (korma/delete m/question)
  (korma/delete m/comment)
  (korma/delete m/objective)
  (korma/delete m/user))

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
  "Creates a fake application context" 
  [] (p/session (core/app core/app-config)))

;; Checkers for peridot responses
(defn headers-location [expected-location]
  (midje/contains
   {:response
    (midje/contains
     {:headers
      (midje/contains
       {"Location" (midje/contains expected-location)})})}))

(defn flash-message-contains [expected-flash-message]
  (midje/contains {:response (midje/contains {:flash (midje/contains expected-flash-message)})}))

(defn peridot-response-json-body->map [peridot-response]
  (-> (get-in peridot-response [:response :body])
      (json/parse-string true)))

(defn check-json-body [expected-json-as-map]
  (fn [peridot-response]
    (let [parsed-body (peridot-response-json-body->map peridot-response)]
      (= parsed-body expected-json-as-map))))
