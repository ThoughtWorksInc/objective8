(ns d-cent.user
  (:require [d-cent.storage :as storage]))

(defn retrieve-user-record [store user-id]
  (storage/find-by store "users" #(= user-id (:user-id %))))

(defn store-user-profile! [store user-profile]
  (storage/store! store "users" user-profile))
