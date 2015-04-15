(ns objective8.users
  (:require [objective8.storage.storage :as storage]))

(defn retrieve-user [user-uri]
  (let [result (storage/pg-retrieve-entity-by-uri user-uri)]
    (when (get result :_id) result)))

(defn find-user-by-twitter-id [twitter-id]
  (-> (storage/pg-retrieve {:entity :user :twitter-id twitter-id})
      :result
      first))

(defn find-user-by-username [username]
  (-> (storage/pg-retrieve {:entity :user :username username}) 
      :result
      first))

(defn store-user! [user]
  (let [user (assoc user :entity :user)]
    (storage/pg-store! user)))

(defn update-user! [user])
