(ns objective8.back-end.domain.users
  (:require [objective8.back-end.storage.storage :as storage]))

(defn retrieve-user [user-uri]
  (let [result (storage/pg-retrieve-entity-by-uri user-uri)]
    (when (get result :_id) result)))

(defn find-user-by-auth-provider-user-id [auth-provider-user-id]
  (-> (storage/pg-retrieve {:entity :user :auth-provider-user-id auth-provider-user-id})
      :result
      first))

(defn find-user-by-username [username]
  (-> (storage/pg-retrieve {:entity :user :username username}) 
      :result
      first))

(defn store-user! [user]
  (-> (assoc user :entity :user)
      storage/pg-store!))

(defn update-user! [user]
  (storage/pg-update-user! user))

(defn store-admin! [admin-data]
  (storage/pg-store! (assoc admin-data :entity :admin)))

(defn get-admin-by-auth-provider-user-id [auth-provider-user-id]
  (-> (storage/pg-retrieve {:entity :admin :auth-provider-user-id auth-provider-user-id})
      :result
      first))
