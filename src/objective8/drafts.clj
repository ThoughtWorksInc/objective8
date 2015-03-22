(ns objective8.drafts
  (:require [objective8.storage.storage :as storage]))

(defn store-draft! [draft]
  (storage/pg-store! (assoc draft :entity :draft)))

(defn retrieve-draft [draft-id]
  (when-let [draft (storage/pg-retrieve-draft-with-id draft-id)]
    (let [objective-id (:objective-id draft)
          created-at (:_created_at_sql_time draft)
          previous-draft-id (-> (storage/pg-retrieve {:entity :draft
                                                      :objective-id objective-id
                                                      :_created_at ['< created-at]})
                                :result
                                last
                                :_id)
          next-draft-id (-> (storage/pg-retrieve {:entity :draft
                                                  :objective-id objective-id
                                                  :_created_at ['> created-at]})
                            :result
                            first
                            :_id)]
      (-> draft
          (dissoc :_created_at_dql_time)
          (assoc :previous-draft-id previous-draft-id :next-draft-id next-draft-id)))))

(defn retrieve-latest-draft [objective-id]
  (-> (storage/pg-retrieve {:entity :draft :objective-id objective-id}
                           {:sort {:field :_created_at :ordering :DESC}})
      :result
      first))

(defn retrieve-drafts [objective-id]
  (->> (storage/pg-retrieve {:entity :draft :objective-id objective-id}
                            {:limit 50
                             :sort {:field :_created_at :ordering :DESC}})
       :result))
