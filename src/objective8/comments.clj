(ns objective8.comments
  (:require [objective8.storage.storage :as storage]
            [objective8.objectives :refer [open?] :as objectives]
            [objective8.utils :as utils]))

(defn replace-comment-on-id [comment comment-on-uri]
  (-> comment
      (assoc :comment-on-uri comment-on-uri)
      (dissoc :comment-on-id)))

(defn uri-for-comment [comment]
  (str "/comments/" (:_id comment)))

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
            (dissoc :global-id)
            (utils/update-in-self [:uri] uri-for-comment)
            (replace-comment-on-id comment-on-uri))))


(defn get-comments-ordered-by [ordered-by entity-uri]
  (when-let [{:keys [global-id]} (storage/pg-retrieve-entity-by-uri entity-uri :with-global-id)]
    (->> (storage/pg-retrieve-comments-with-votes-ordered-by global-id ordered-by)
         (map #(dissoc % :global-id))
         (map #(utils/update-in-self % [:uri] uri-for-comment))
         (map #(replace-comment-on-id % entity-uri)))))
