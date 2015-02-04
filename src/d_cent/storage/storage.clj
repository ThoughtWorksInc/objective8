(ns d-cent.storage.storage
  (:require [korma.core :as korma]
            [d-cent.storage.mappings :as mappings]))

(defn pg-store! [{:keys [entity] :as m}]
  (if-let [ent (get mappings/entities entity)]
    (korma/insert ent (korma/values (dissoc m :entity)))
    (throw (Exception. "Could not find database mapping for " entity))))

(defn request->store
  "Fetches the storage atom from the request"
  [request]
  (:store (:d-cent request)))

(defn gen-uuid [] (str (java.util.UUID/randomUUID)))

(defn retrieve
  "Retrieves a record from the in memory database"
  [store collection id]
  (first (filter #(= (:_id %) id) (get @store collection))))

(defn store! 
  "Stores a record in the in memory database"
  [store collection record]
  (let [record-to-save (assoc record :_id (gen-uuid))]
    (swap! store update-in [collection] conj record-to-save)
    record-to-save))

(defn find-by 
  "Retrieves the first record to match predicate"
  [store collection predicate]
  (first (filter predicate (get @store collection))))
