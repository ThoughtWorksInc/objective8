(ns objective8.config
  (:require [clojure.tools.logging :as log]))

(def ^:dynamic enable-csrf true)
(def ^:dynamic two-phase? false)

(defn- env-lookup [var-name]
  (get (System/getenv) var-name))

(defn get-var
  "Attempts to read an environment variable. If no variable is
  found will log a warning message and use the default. If no
  default is provided will use nil"
  ([var-name]
   (get-var var-name nil))
  ([var-name default]
   (get-var var-name default true))
  ([var-name default logs?]
   (if-let [variable (get (System/getenv) var-name)]
     variable
     (do
       (if default
         (when logs? (log/info (str "Failed to look up environment variable \"" var-name "\" - using provided default")))
         (when logs? (log/error (str "Failed to look up environment variable \"" var-name "\" - no default provided"))))
       default))))

(def base-uri (get-var "BASE_URI" "localhost:8080"))

(defn get-private-mode []
  (let [twitter-token (get-var "TWITTER_CONSUMER_TOKEN" nil false)
        twitter-secret-token (get-var "TWITTER_CONSUMER_SECRET_TOKEN" nil false)
        facebook-id (get-var "FB_CLIENT_ID" nil false)
        facebook-secret (get-var "FB_CLIENT_SECRET" nil false)
        private-mode (get-var "PRIVATE_MODE_ENABLED" false)]
    (when (and twitter-secret-token twitter-token private-mode) (log/warn (str "Private mode enabled. Twitter login will be disabled")))
    (when (and facebook-id facebook-secret private-mode) (log/warn (str "Private mode enabled. Facebook login will be disabled")))
    private-mode
    ))

(def ^:dynamic environment
  {:uri-scheme                    (get-var "URI_SCHEME" "http")
   :base-uri                      base-uri

   :front-end-uri                 base-uri
   :front-end-port                (Integer/parseInt (get-var "APP_PORT" "8080"))

   :api-uri                       (get-var "API_URI" "localhost:8081")
   :api-port                      (Integer/parseInt (get-var "API_SERVER_PORT" "8081"))

   :fake-twitter-mode             (get-var "FAKE_TWITTER_MODE")
   :api-credentials               {:bearer-name  (get-var "API_BEARER_NAME")
                                   :bearer-token (get-var "API_BEARER_TOKEN")}
   :admins                        (get-var "ADMINS")
   :db-config                     {:db       (or (get-var "POSTGRES_DB") (get-var "DB_NAME" "objective8"))
                                   :user     (or (get-var "POSTGRES_USER") (get-var "DB_USER" "objective8"))
                                   :password (or (get-var "POSTGRES_PASSWORD") (get-var "DB_PASSWORD" "development")) ;TODO password management
                                   :host     (or (get-var "POSTGRES_PORT_5432_TCP_ADDR") (get-var "DB_HOST" "localhost"))
                                   :port     (or (get-var "POSTGRES_PORT_5432_TCP_PORT") (get-var "DB_PORT" 5432))}
   :google-analytics-tracking-id  (get-var "GA_TRACKING_ID")
   :twitter-credentials           {:consumer-token (get-var "TWITTER_CONSUMER_TOKEN")
                                   :secret-token   (get-var "TWITTER_CONSUMER_SECRET_TOKEN")}
   :facebook-credentials          {:client-id     (get-var "FB_CLIENT_ID")
                                   :client-secret (get-var "FB_CLIENT_SECRET")}
   :okta-credentials              {:client-id     (get-var "OKTA_CLIENT_ID")
                                   :client-secret (get-var "OKTA_CLIENT_SECRET")
                                   :auth-url      (get-var "OKTA_AUTH_URL")}
   :stonecutter-auth-provider-url (get-var "STONECUTTER_AUTH_URL")
   :stonecutter-client-id         (get-var "STONECUTTER_CLIENT_ID")
   :stonecutter-client-secret     (get-var "STONECUTTER_CLIENT_SECRET")
   :stonecutter-admin-email       (get-var "STONECUTTER_ADMIN_EMAIL" "")
   :coracle-bearer-token          (get-var "CORACLE_BEARER_TOKEN")
   :coracle-post-uri              (get-var "CORACLE_URI")
   :app-name                      (get-var "APP_NAME" "Objective[8]")
   :stonecutter-name              (get-var "STONECUTTER_NAME" "Stonecutter")
   :show-alpha-warnings           (get-var "SHOW_ALPHA_WARNINGS" false)
   :cookie-message-enabled        (get-var "COOKIE_MESSAGE_ENABLED" false)
   :private-mode-enabled          (get-private-mode)})

(def replacement-keys [:app-name :stonecutter-name :stonecutter-admin-email])