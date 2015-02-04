(ns d-cent.storage.mappings
  (:require [korma.core :as korma]
            [cheshire.core :as json])
  (:import [org.postgresql.util PGobject]))

(defn map->json-type
  "Convert a clojure map to a Postgres JSON type"
  [m]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (json/generate-string m))))

(defn map->objective
  "Converts a clojure map into a json-typed objective for the database"
  [{:keys [created-by] :as objective}]
  (if created-by
    {:created_by created-by
     :objective (map->json-type objective)} 
    (throw (Exception. "Could not transform map to objective"))))

(korma/defentity objective
  (korma/pk :_id)
  (korma/table :policy_drafting.objectives)
  (korma/prepare map->objective))

(def entities {:objective objective})
