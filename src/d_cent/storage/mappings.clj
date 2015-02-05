(ns d-cent.storage.mappings
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

(defn map->objective
  "Converts a clojure map into a json-typed objective for the database"
  [{:keys [created-by end-date] :as objective}]
  (if (and created-by end-date) 
    {:created_by created-by
     :end_date (tc/to-timestamp end-date)
     :objective (map->json-type objective)} 
    (throw (Exception. "Could not transform map to objective"))))

(defn map->user
  "Converts a clojure map into a json-typed user for the database"
  [{:keys [user-id] :as user}]
  (if user-id
    {:user_id user-id
     :user_data (map->json-type user)}
    (throw (Exception. "Could not transform map to user"))))

(korma/defentity objective
  (korma/pk :_id)
  (korma/table :policy_drafting.objectives)
  (korma/prepare map->objective))

(korma/defentity user
  (korma/pk :_id)
  (korma/table :policy_drafting.users)
  (korma/prepare map->user))

(def entities {:objective objective
               :user      user})

(defn get-mapping
  "Returns a korma entity for a map"
  [{:keys [entity]}]
  (get entities entity))
