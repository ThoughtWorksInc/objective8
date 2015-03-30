(ns objective8.comments
  (:require [objective8.storage.storage :as storage]
            [objective8.objectives :refer [open?] :as objectives]
            [objective8.utils :as utils]))

(defn replace-comment-on-id [comment comment-on-uri]
  (-> comment
      (assoc :comment-on-uri comment-on-uri)
      (dissoc :comment-on-id)))

(defn store-comment-for! [entity-to-comment-on
                          {:keys [comment-on-uri] :as comment-data}]
  (when-let [{:keys [objective-id _id global-id]} entity-to-comment-on]
    (some-> comment-data
            (utils/select-all-or-nothing [:comment :created-by-id])
            (assoc :entity :comment
                   :comment-on-id global-id
                   :objective-id (or objective-id _id))
            (dissoc :comment-on-uri)
            storage/pg-store!
            (replace-comment-on-id comment-on-uri))))

(defn get-comments [entity-uri]
  (when-let [{:keys [global-id]} (storage/pg-retrieve-entity-by-uri entity-uri :with-global-id)]
    (->> (storage/pg-retrieve {:entity :comment
                               :comment-on-id global-id}
                              {:limit 50
                               :sort {:field :_created_at
                                      :ordering :DESC}})
         :result
         (map #(replace-comment-on-id % entity-uri)))))
