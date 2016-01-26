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
  (if-let [variable (get (System/getenv) var-name)]
    variable
    (do
      (if default
          (log/info (str "Failed to look up environment variable \"" var-name "\" - using provided default"))
          (log/error (str "Failed to look up environment variable \"" var-name "\" - no default provided")))
      default))))

(def base-uri (get-var "BASE_URI" "localhost:8080"))

(def ^:dynamic environment
  {:uri-scheme                    (get-var "URI_SCHEME" "http")
   :base-uri                      base-uri

   :front-end-uri                 base-uri
   :front-end-port                (Integer/parseInt (get-var "APP_PORT" "8080"))

   :api-uri                       (get-var "API_URI" "localhost:8081")
   :external-api-uri              (get-var "EXTERNAL_API_URI" "localhost:8081")
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
   :stonecutter-auth-provider-url (get-var "STONECUTTER_AUTH_URL")
   :stonecutter-client-id         (get-var "STONECUTTER_CLIENT_ID")
   :stonecutter-client-secret     (get-var "STONECUTTER_CLIENT_SECRET")
   :coracle-bearer-token          (get-var "CORACLE_BEARER_TOKEN")
   :coracle-post-uri              (get-var "CORACLE_URI")
   :favicon-file-name             (get-var "FAVICON_FILE_NAME")
   :app-name                      (get-var "APP_NAME" "Objective[8]")
   :show-alpha-warnings           (get-var "ALPHA_WARNINGS" false)})

(def replacement-keys [:app-name])