(ns d-cent.storage.database
  (:require [korma.db :as db]))

(def postgres-spec (db/postgres {:db "dcent"
                                 :user "dcent"
                                 :password "development" ;TODO password management
                                 :host "localhost"}))

(defn connect!
  "Connect to the database described by the DB spec"
  [dbspec]
  (db/default-connection (db/create-db dbspec)))
