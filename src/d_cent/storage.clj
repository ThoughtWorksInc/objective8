(ns d-cent.storage
  (:require [korma.core :as korma]
            [korma.db :as db]
            [cheshire.core :as json])
  (:import [org.postgresql.util PGobject]))

(def dbspec (db/postgres {:db "dcent"
                          :user "dcent"
                          :password "development"
                          :host "localhost"}))

(defn map->json-type
  "Convert a clojure map to a Postgres JSON type"
  [m]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (json/generate-string m)))) 

(defn connect! []
  (db/default-connection (db/create-db dbspec))) 

(korma/defentity objective
  (korma/pk :_id)
  (korma/table :policy_drafting.objectives)
  (korma/prepare (fn [{:keys [created-by] :as objective}]
                   {:created_by created-by
                    :objective (map->json-type objective)})))

(def database-mappings {:objective objective})

(defn stuff []
  (korma/insert objective (korma/values {:created-by "foo" :something "bar"}))) 

(defn pg-store! [{:keys [entity] :as m}]
  (if-let [ent (get database-mappings entity)]
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
