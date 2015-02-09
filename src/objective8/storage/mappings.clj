(ns objective8.storage.mappings
  (:require [korma.core :as korma]
            [cheshire.core :as json]
            [clj-time [format :as tf] [coerce :as tc]])
  (:import [org.postgresql.util PGobject]))

(defn map->json-type
  "Convert a clojure map to a Postgres JSON type"
  [m]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (json/generate-string m))))

(defn json-type->map
  "Convert a Postgres JSON type to a clojure map"
  [pgobject]
  (json/parse-string (.getValue pgobject) true))

(defn map->objective
  "Converts a clojure map into a json-typed objective for the database"
  [{:keys [created-by-id end-date] :as objective}]
  (if (and created-by-id end-date)
    {:created_by_id created-by-id
     :end_date (tc/to-timestamp end-date)
     :objective (map->json-type objective)}
    (throw (Exception. "Could not transform map to objective"))))

(defn map->comment
  "Converts a clojure map into a json-typed comment for the database"
  [{:keys [created-by-id root-id parent-id] :as comment}]
  (if (and created-by-id root-id parent-id)
    {:created_by_id created-by-id
     :root_id root-id
     :parent_id parent-id
     :comment (map->json-type comment)}
    (throw (Exception. "Could not transform map to comment"))))

(defn map->user
  "Converts a clojure map into a json-typed user for the database"
  [{:keys [twitter-id] :as user}]
  (if twitter-id
    {:twitter_id twitter-id
     :user_data (map->json-type user)}
    (throw (Exception. "Could not transform map to user"))))

(defn unmap [data-key]
  (fn [m] (assoc (json-type->map (data-key m)) :_id (:_id m))))

(korma/defentity objective
  (korma/pk :_id)
  (korma/table :policy_drafting.objectives)
  (korma/prepare map->objective)
  (korma/transform (unmap :objective)))

(korma/defentity user
  (korma/pk :_id)
  (korma/table :policy_drafting.users)
  (korma/prepare map->user)
  (korma/transform (unmap :user_data)))

(korma/defentity objective8.storage.mappings/comment
  (korma/pk :_id)
  (korma/table :policy_drafting.comments)
  (korma/prepare map->comment)
  (korma/transform (unmap :comment)))

(def entities {:objective objective
               :user      user
               :comment   objective8.storage.mappings/comment})

(defn get-mapping
  "Returns a korma entity for a map"
  [{:keys [entity]}]
  (get entities entity))
