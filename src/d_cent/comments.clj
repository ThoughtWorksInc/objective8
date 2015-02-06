(ns d-cent.comments
  (:require [cemerick.friend :as friend]
            [d-cent.storage.storage :as storage]))
(defn request->comment
  [{:keys [params]}]
    (assoc (select-keys params [:comment :objective-id])
                                :user-id (get (friend/current-authentication) :username)))

(defn store-comment! [store comment]
 (storage/store! store "comments" comment))
