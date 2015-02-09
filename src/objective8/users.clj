(ns objective8.users
  (:require [objective8.storage.storage :as storage]))

(defn retrieve-user [user-id]
  (let [{result :result} (storage/pg-retrieve {:entity :user :_id user-id})]
    (dissoc (first result) :entity)))

(defn find-user-by-twitter-id [twitter-id]
  (let [{result :result} (storage/pg-retrieve {:entity :user :twitter-id twitter-id})]
    (dissoc (first result) :entity)))

(defn store-user! [user]
  (let [user (assoc user :entity :user)]
    (storage/pg-store! user)))
