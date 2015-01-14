(ns d-cent.storage
  (:require [com.ashafa.clutch :as c]))

(defn retrieve
  "Retrieves the latest version of a document in the database"
  [db id]
  (c/get-document db id))

(defn store! 
  "Creates or updates a document in the database"
  [db document]
  (if-let [existing (retrieve db (:_id document))]
    (c/put-document db (assoc document :_rev (:_rev existing))) 
    (c/put-document db document)))
