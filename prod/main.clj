(ns main
  (:require [objective8.core :as core]
            [objective8.back-end.storage.database :as db]
            [ragtime.main :as rm]
            [ragtime.sql.database])
  (:gen-class)) 

(def db-string (str "jdbc:postgresql://" (:host db/db-config)
                    ":" (:port db/db-config)
                    "/" (:db db/db-config)
                    "?user=" (:user db/db-config)
                    "&&password=" (:password db/db-config)))
   
(defn -main []
  (rm/migrate {:database db-string
               :migrations "ragtime.sql.files/migrations"})
  (core/start-server))
