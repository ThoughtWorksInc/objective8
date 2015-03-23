(ns objective8.drafts
  (:require [objective8.storage.storage :as storage]))

(defn store-draft! [draft]
  (storage/pg-store! (assoc draft :entity :draft)))

(defn retrieve-previous-draft [draft]
  (-> (storage/pg-retrieve {:entity :draft
                            :objective-id (:objective-id draft) 
                            :_created_at ['< (:_created_at_sql_time draft)]})
      :result
      last))

(defn retrieve-next-draft [draft]
  (-> (storage/pg-retrieve {:entity :draft
                            :objective-id (:objective-id draft)
                            :_created_at ['> (:_created_at_sql_time draft)]})
      :result
      first))

(defn retrieve-draft [draft-id]
  (when-let [draft (-> (storage/pg-retrieve {:entity :draft 
                                         :_id draft-id})
                       :result
                       first)]
    (let [previous-draft-id (:_id (retrieve-previous-draft draft))
          next-draft-id (:_id (retrieve-next-draft draft))]
      (-> draft
          (dissoc :_created_at_sql_time)
          (assoc :previous-draft-id previous-draft-id :next-draft-id next-draft-id)))))

(defn retrieve-latest-draft [objective-id]
  (when-let [latest-draft (-> (storage/pg-retrieve {:entity :draft :objective-id objective-id}
                                                   {:sort {:field :_created_at :ordering :DESC}})
                              :result
                              first)]
    (let [previous-draft-id (:_id (retrieve-previous-draft latest-draft))]
      (-> latest-draft
          (dissoc :_created_at_sql_time)
          (assoc :previous-draft-id previous-draft-id)))))

(defn retrieve-drafts [objective-id]
  (->> (storage/pg-retrieve {:entity :draft :objective-id objective-id}
                           {:limit 50
                            :sort {:field :_created_at :ordering :DESC}})
      :result
      (map #(dissoc % :created_at_sql_time))))
