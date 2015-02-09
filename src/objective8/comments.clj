(ns objective8.comments
  (:require [cemerick.friend :as friend]
            [objective8.storage.storage :as storage]))

(defn request->comment
  [{:keys [params]}]
  (let [root-id (Integer/parseInt (params :root-id))
        parent-id (Integer/parseInt (params :parent-id))]
    (zipmap [:comment :root-id :parent-id :created-by-id]
            [(params :comment) root-id parent-id (get (friend/current-authentication) :username)])))

(defn store-comment! [comment]
 (storage/pg-store! (assoc comment :entity :comment)))
