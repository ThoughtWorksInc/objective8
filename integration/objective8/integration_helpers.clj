(ns objective8.integration-helpers
  (:require [net.cgrand.enlive-html :as html]
            [midje.sweet :as midje]
            [korma.core :as korma]
            [peridot.core :as p]
            [cheshire.core :as json]
            [clojure.data.json :as cl-json]
            [objective8.core :as core]
            [objective8.storage.mappings :as m]
            [objective8.storage.database :as db]))

(defn db-connection [] (db/connect!))

(defn truncate-tables []
  (korma/delete m/bearer-token)
  (korma/delete m/candidate)
  (korma/delete m/invitation)
  (korma/delete m/answer)
  (korma/delete m/question)
  (korma/delete m/comment)
  (korma/delete m/draft)
  (korma/delete m/objective)
  (korma/delete m/user))

(defn get-anti-forgery-token [request-context]
  (let [raw-html (:body (:response request-context))]
   (do
    (get-in (first (html/select (html/html-snippet raw-html) [:#__anti-forgery-token])) [:attrs :value]))))


(defn with-sign-in [user-session url & args]
  (let [request-function #(apply p/request % url args)]
    (-> user-session
        ; Hit unauthorized url
        request-function
        ; Retrieve twitter user-name (NB: requires twitter authentication background to be set)
        (p/request "http://localhost:8080/twitter-callback?oauth_verifier=the-verifier")
        ; Post user email address to store --- returns authentication map
        (p/request "http://localhost:8080/sign-up" :request-method :post 
                   :content-type "application/x-www-form-urlencoded"
                   :body "&username=someusername&email-address=test%40email.address.com")
        ; Follow redirect to originally requested resource
        (p/follow-redirect))))

(defn sign-in-as-existing-user [user-session]
 (-> user-session
     (p/request "http://localhost:8080/twitter-callback?oauth_verifier=the-verifier")
     p/follow-redirect))

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
  (json/parse-string (get-in peridot-response [:response :body]) true))

(defn check-json-body [expected-json-as-map]
   (midje/chatty-checker [peridot-response]
      (= (peridot-response-json-body->map peridot-response)
         expected-json-as-map)))


(defn json-contains [expected]
  (midje/chatty-checker [actual]
                        ((midje/contains expected)
                         (cl-json/read-str actual :key-fn keyword))))

(defn location-contains [expected]
  (midje/contains {"Location" (midje/contains expected)}))
