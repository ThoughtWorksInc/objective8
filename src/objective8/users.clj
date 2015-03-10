(ns objective8.users
  (:require [objective8.storage.storage :as storage]
            [objective8.writers :as writers]))

(defn retrieve-user [user-id]
  (let [candidates (writers/retrieve-candidates-by-user-id user-id)]
    (some-> (storage/pg-retrieve {:entity :user :_id user-id})
            :result
            first
            (assoc :writer-records candidates))))

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
