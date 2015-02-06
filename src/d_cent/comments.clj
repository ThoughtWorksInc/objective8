(ns d-cent.comments
  (:require [cemerick.friend :as friend]
            [d-cent.storage.storage :as storage]))
(defn request->comment
  [{:keys [params]}]
  (-> (select-keys params [:comment :objective-id])
      (assoc :user-id (get (friend/current-authentication) :username))
      (update-in [:objective-id] #(Integer/parseInt %))))

(defn store-comment! [store comment]
 (storage/store! store "comments" comment))
