(ns objective8.integration.integration-helpers
  (:require [net.cgrand.enlive-html :as html]
            [midje.sweet :as midje]
            [korma.core :as korma]
            [peridot.core :as p]
            [cheshire.core :as json]
            [clojure.data.json :as cl-json]
            [clojure.tools.logging :as log]
            [ring.middleware.session.store :as rss]
            [objective8.core :as core]
            [objective8.front-end.workflows.twitter :as twitter]
            [objective8.front-end.workflows.sign-up :as sign-up]
            [objective8.back-end.storage.mappings :as m]
            [objective8.back-end.storage.database :as db]))

(defn db-connection [] (db/connect!))

(defn truncate-tables []
  (korma/delete m/reason)
  (korma/delete m/admin-removal)
  (korma/delete m/admin)
  (korma/delete m/section)
  (korma/delete m/star)
  (korma/delete m/bearer-token)
  (korma/delete m/up-down-vote)
  (korma/delete m/writer-note)
  (korma/delete m/writer)
  (korma/delete m/invitation)
  (korma/delete m/answer)
  (korma/delete m/mark)
  (korma/delete m/question)
  (korma/delete m/comment)
  (korma/delete m/draft)
  (korma/delete m/objective)
  (korma/delete m/global-identifier)
  (korma/delete m/user))

(defn get-anti-forgery-token [request-context]
  (let [raw-html (:body (:response request-context))]
   (do
    (get-in (first (html/select (html/html-snippet raw-html) [:#__anti-forgery-token])) [:attrs :value]))))


(defn with-sign-in
  "CAUTION - this method requires that the oauth flow has been provided over in midje
             otherwise it will throw null pointer exceptions FIXME"
  [user-session url & args]
  (let [request-function #(apply p/request % url args)]
    (-> user-session
        ; Hit unauthorized url
        request-function
        ; Retrieve twitter user-name (NB: requires twitter authentication background to be set)
        (p/request "http://localhost:8080/twitter-callback?oauth_verifier=the-verifier")
        ; Post user email address to store --- returns authentication map
        (p/request "http://localhost:8080/sign-up" :request-method :post
                   :content-type "application/x-www-form-urlencoded"
                   :body "username=someusername&email-address=test%40email.address.com")
        ; Follow redirect to originally requested resource
        (p/follow-redirect))))

(defn sign-in-as-existing-user [user-session]
 (-> user-session
     (p/request "http://localhost:8080/twitter-callback?oauth_verifier=the-verifier")
     p/follow-redirect))

;; Session store implementation for testing
(deftype TestSessionStore [session-atom]
  rss/SessionStore
  (read-session [_ key]
    (log/info "read-session" @session-atom key)
    (@session-atom key))
  (write-session [_ key data]
    (let [key (or key "dummy-key")]
    (log/info "write-session" @session-atom key data)
    (swap! session-atom assoc key data)
    key))
  (delete-session [_ key]
    (log/info "delete-session" @session-atom)
    (swap! session-atom dissoc key)
    nil))

(defn test-session-store [session-atom]
  (TestSessionStore. session-atom))

(def session-atom (atom nil))

;; App configuration for integration tests
(def test-config
  (assoc core/app-config
         :authentication {:allow-anon? true
                          :workflows [(twitter/twitter-workflow {})
                                      sign-up/sign-up-workflow]
                          :login-uri "/sign-in"}
         :https nil))

(defn front-end-context
  "Creates a fake application context"
  ([] (front-end-context {}))

  ([config-changes]
   (p/session (core/front-end-handler (merge test-config config-changes)))))

(defn api-context
  "Creates a fake application context"
  ([] (api-context {}))

  ([config-changes]
   (p/session (core/back-end-handler (merge test-config config-changes)))))

;; Test data generators

(defn string-of-length [l]
  (apply str (repeat l "x")))

;; Test helpers
(defn count-matches [the-string target-regex]
  (count (re-seq (re-pattern target-regex) the-string)))

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

(defn parse-json-string-with-entity-value-as-keyword [json-string]
  (cl-json/read-str json-string
                    :key-fn keyword
                    :value-fn (fn [k v] ((if (= k :entity) keyword identity) v))))

(defn json-contains [expected & options]
  (midje/chatty-checker [actual]
                         ((apply midje/contains expected options)
                          (parse-json-string-with-entity-value-as-keyword actual))))

(defn location-contains [expected]
  (midje/contains {"Location" (midje/contains expected)}))

(def no-untranslated-strings
  (let [untranslated-string-regex #"(?!!DOCTYPE|!IEMobile)!\w+"]
    (midje/chatty-checker [response-body] (empty? (re-seq untranslated-string-regex response-body)))))

