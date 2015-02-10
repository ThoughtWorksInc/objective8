(ns objective8.comments
  (:require [cemerick.friend :as friend]
            [objective8.storage.storage :as storage]))

(defn request->comment
  [{:keys [params]}]
  (let [objective-id (Integer/parseInt (params :objective-id))]
    (zipmap [:comment :objective-id :created-by-id]
            [(params :comment) objective-id (get (friend/current-authentication) :username)])))

(defn store-comment! [comment]
 (storage/pg-store! (assoc comment :entity :comment)))
