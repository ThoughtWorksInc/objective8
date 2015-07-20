(ns objective8.back-end.domain.users
  (:require [objective8.back-end.storage.storage :as storage]))

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
  (let [auth-provider-user-id (:auth-provider-user-id user)
        user (cond-> user
               auth-provider-user-id (assoc :twitter-id auth-provider-user-id)
               auth-provider-user-id (dissoc :auth-provider-user-id)   
               true (assoc :entity :user))]
    (storage/pg-store! user)))

(defn update-user! [user]
  (storage/pg-update-user! user))

(defn store-admin! [admin-data]
  (storage/pg-store! (assoc admin-data :entity :admin)))

(defn get-admin-by-twitter-id [twitter-id]
  (-> (storage/pg-retrieve {:entity :admin :twitter-id twitter-id})
      :result
      first))
