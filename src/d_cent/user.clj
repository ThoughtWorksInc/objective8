(ns d-cent.user
  (:require [d-cent.storage.storage :as storage]))

(defn retrieve-user [user-id]
  (let [{result :result} (storage/pg-retrieve {:entity :user :_id user-id})]
    (dissoc (first result) :entity)))

(defn find-user-by-twitter-id [twitter-id]
  (let [{result :result} (storage/pg-retrieve {:entity :user :twitter-id twitter-id})]
    (dissoc (first result) :entity)))

(defn store-user! [user-profile]
  (let [user (assoc user-profile :entity :user)]
    (storage/pg-store! user)))
