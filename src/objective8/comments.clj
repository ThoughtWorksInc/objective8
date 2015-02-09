(ns objective8.comments
  (:require [cemerick.friend :as friend]
            [objective8.storage.storage :as storage]))

(defn request->comment
  [{:keys [params]}]
  (let [discussing-id (Integer/parseInt (params :objective-id))
        parent-id discussing-id]
    (zipmap [:comment :discussing-id :parent-id :created-by-id]
            [(params :comment) discussing-id parent-id (get (friend/current-authentication) :username)])))

(defn store-comment! [comment]
 (storage/pg-store! (assoc comment :entity :comment)))
