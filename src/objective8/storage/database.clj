(ns objective8.storage.database
  (:require [korma.db :as db]
            [clojure.tools.logging :as log]
            [objective8.config :as config]))

(def postgres-spec (db/postgres {:db (config/get-var "DB_NAME" "objective8")
                                 :user (config/get-var "DB_USER" "objective8")
                                 :password (config/get-var "DB_PASSWORD" "development") ;TODO password management
                                 :host (config/get-var "DB_HOST" "localhost")
                                 :port (config/get-var "DB_PORT" 5432)}))

(defn connect!
  "Connect to the database described by the DB spec"
  ([]
   (connect! postgres-spec))

  ([spec]
   (log/info (str "Attempting to connect to the database" (dissoc spec :password)))
   (db/defdb objective8-db spec)))
