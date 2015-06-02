(ns objective8.back-end.storage.database
  (:require [korma.db :as db]
            [clojure.tools.logging :as log]
            [objective8.config :as config]))

(def db-config (:db-config config/environment))

(def postgres-spec (db/postgres db-config))

(def spec db/postgres)

(defn connect!
  "Connect to the database described by the DB spec"
  ([]
   (connect! postgres-spec))

  ([spec]
   (log/info (str "Attempting to connect to the database" (dissoc spec :password)))
   (db/defdb objective8-db spec)))
