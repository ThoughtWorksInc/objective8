(ns objective8.storage.mappings
  (:refer-clojure :exclude [comment])
  (:require [korma.core :as korma]
            [cheshire.core :as json]
            [clj-time [format :as tf] [coerce :as tc]]
            [objective8.utils :as utils])
  (:import [org.postgresql.util PGobject]))

(defn sql-time->iso-time-string [sql-time]
  (utils/date-time->iso-time-string (tc/from-sql-time sql-time)))


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
  [{:keys [created-by-id objective-id] :as comment}]
  (if (and created-by-id objective-id)
    {:created_by_id created-by-id
     :objective_id objective-id
     :comment (map->json-type comment)}
    (throw (Exception. "Could not transform map to comment"))))

(defn map->user
  "Converts a clojure map into a json-typed user for the database"
  [{:keys [twitter-id] :as user}]
  (if twitter-id
    {:twitter_id twitter-id
     :user_data (map->json-type user)}
    (throw (Exception. "Could not transform map to user"))))

(defn map->question
  "Converts a clojure map into a json-typed question for the database"
  [{:keys [created-by-id objective-id] :as question}]
  (if (and created-by-id objective-id)
    {:created_by_id created-by-id
     :objective_id objective-id
     :question (map->json-type question)}
    (throw (Exception. "Could not transform map to question"))))

(defn unmap [data-key]
  (fn [m] (assoc (json-type->map (data-key m)) 
                 :_id (:_id m)
                 :_created_at (sql-time->iso-time-string (:_created_at m)))))

(korma/defentity objective
  (korma/pk :_id)
  (korma/table :objective8.objectives)
  (korma/prepare map->objective)
  (korma/transform (unmap :objective)))

(korma/defentity user
  (korma/pk :_id)
  (korma/table :objective8.users)
  (korma/prepare map->user)
  (korma/transform (unmap :user_data)))

(korma/defentity comment
  (korma/pk :_id)
  (korma/table :objective8.comments)
  (korma/prepare map->comment)
  (korma/transform (unmap :comment)))

(korma/defentity question
  (korma/pk :_id)
  (korma/table :objective8.questions)
  (korma/prepare map->question)
  (korma/transform (unmap :question)))

(def entities {:objective objective
               :user      user
               :comment   comment
               :question  question})

(defn get-mapping
  "Returns a korma entity for a map"
  [{:keys [entity]}]
  (get entities entity))
