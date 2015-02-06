(ns d-cent.storage.database
  (:require [korma.db :as db]
            [d-cent.config :as config]))

(def postgres-spec (db/postgres {:db "dcent"
                                 :user (config/get-var "SNAP_DB_PG_USER" "dcent")
                                 :password (config/get-var "SNAP_DB_PG_PASSWORD" "development") ;TODO password management
                                 :host (config/get-var "SNAP_DB_PG_HOST" "localhost")
                                 :port (config/get-var "SNAP_DB_PG_PORT" 5432)}))

(defn connect!
  "Connect to the database described by the DB spec"
  [dbspec]
  (db/default-connection (db/create-db dbspec)))
