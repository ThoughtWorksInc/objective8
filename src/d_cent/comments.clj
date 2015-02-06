(ns d-cent.comments
  (:require [cemerick.friend :as friend]))

(defn request->comment
  [{:keys [params]}]
    (assoc (select-keys params [:comment :objective-id])
                                :user-id (get (friend/current-authentication) :username)))
