(ns objective8.comments
  (:require [objective8.storage.storage :as storage]
            [objective8.objectives :refer [open?] :as objectives]
            [objective8.utils :as utils]))

(defn store-comment! [{:keys [comment-on-uri] :as comment-data}]
  (when-let [{:keys [objective-id global-id]} (storage/pg-retrieve-entity-by-uri comment-on-uri)]
    (some-> comment-data
            (utils/select-all-or-nothing [:comment
                                          :created-by-id])
            (assoc :entity :comment
                   :comment-on-id global-id
                   :objective-id objective-id)
            (dissoc :comment-on-uri)
            storage/pg-store!
            (dissoc :comment-on-id)
            (assoc :comment-on-uri comment-on-uri))))

(defn create-comment-on-objective! [{objective-id :objective-id :as comment}]
  (when (open? (objectives/retrieve-objective objective-id))
    (storage/pg-store! (assoc comment :entity :comment))))

(defn retrieve-comments [comment-on-id]
  (:result (storage/pg-retrieve {:entity :comment 
                                 :comment-on-id comment-on-id}
                                 {:limit 50})))
