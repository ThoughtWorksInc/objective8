(ns objective8.comments
  (:require [objective8.storage.storage :as storage]
            [objective8.objectives :refer [open?] :as objectives]))

(defn store-comment! [comment]
 (storage/pg-store! (assoc comment :entity :comment)))

(defn create-comment-on-objective! [{objective-id :objective-id :as comment}]
  (when (open? (objectives/retrieve-objective objective-id))
    (store-comment! comment)))

(defn retrieve-comments [comment-on-id]
  (:result (storage/pg-retrieve {:entity :comment 
                                 :comment-on-id comment-on-id}
                                 {:limit 50})))
