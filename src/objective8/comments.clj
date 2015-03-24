(ns objective8.comments
  (:require [objective8.storage.storage :as storage]
            [objective8.objectives :refer [open?] :as objectives]
            [objective8.utils :as utils]))

(defn replace-global-id [comment comment-on-uri]
  (-> comment
      (assoc :comment-on-uri comment-on-uri)
      (dissoc :comment-on-id)))

(defn store-comment! [{:keys [comment-on-uri] :as comment-data}]
  (when-let [{:keys [objective-id _id global-id]} (storage/pg-retrieve-entity-by-uri comment-on-uri)]
    (some-> comment-data
            (utils/select-all-or-nothing [:comment
                                          :created-by-id])
            (assoc :entity :comment
                   :comment-on-id global-id
                   :objective-id (or objective-id _id))
            (dissoc :comment-on-uri)
            storage/pg-store!
            (replace-global-id comment-on-uri))))

(defn create-comment-on-objective! [{objective-id :objective-id :as comment}]
  (when (open? (objectives/retrieve-objective objective-id))
    (storage/pg-store! (assoc comment :entity :comment))))

(defn retrieve-comments [comment-on-id]
  (:result (storage/pg-retrieve {:entity :comment 
                                 :comment-on-id comment-on-id}
                                 {:limit 50})))

(defn get-comments [entity-uri]
  (when-let [{:keys [global-id]} (storage/pg-retrieve-entity-by-uri entity-uri)]
    (->> (storage/pg-retrieve {:entity :comment
                               :comment-on-id global-id}
                              {:limit 50})
         :result
         (map #(replace-global-id % entity-uri)))))
