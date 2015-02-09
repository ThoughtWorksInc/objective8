(ns objective8.storage.storage
  (:require [korma.core :as korma]
            [objective8.storage.mappings :as mappings]))

(defn insert
  "Wrapper around Korma's insert call"
  [entity data]
  (korma/insert entity (korma/values data)))

(defn pg-store!
  "Transform a map according to its :entity value and save it in the database"
  [{:keys [entity] :as m}]
  (if-let [ent (mappings/get-mapping m)]
    (insert ent m)
    (throw (Exception. (str "Could not find database mapping for " entity)))))

(defn select
  "Wrapper around Korma's select call"
  [entity where]
  (korma/select entity (korma/where where)))

(defn- -to_
  "Replaces hyphens in keys with underscores"
  [m]
  (let [ks (keys m) vs (vals m)]
    (zipmap (map (fn [k] (-> (clojure.string/replace k #"-" "_")
                             (subs 1)
                             keyword)) ks)
            vs)))

(defn pg-retrieve
  "Retrieves objects from the database based on a query map

   - The map must include an :entity key
   - Hyphens in key words are replaced with underscores"
  [{:keys [entity] :as query}]
  (if entity
    (let [result (select (mappings/get-mapping query) (-to_ (dissoc query :entity)))]
      {:query query
       :result result})
    (throw (Exception. "Query map requires an :entity key"))))

(defn request->store
  "Fetches the storage atom from the request"
  [request]
  (:store (:objective8 request)))

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
