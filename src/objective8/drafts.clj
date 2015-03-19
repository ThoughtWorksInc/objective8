(ns objective8.drafts
  (:require [objective8.storage.storage :as storage]))

(defn store-draft! [draft]
  (storage/pg-store! (assoc draft :entity :draft)))

(defn retrieve-draft [draft-id]
  (-> (storage/pg-retrieve {:entity :draft :_id draft-id})
      :result
      first))

(defn retrieve-latest-draft [objective-id]
  (-> (storage/pg-retrieve {:entity :draft :objective-id objective-id}
                           {:sort {:field :_created_at :ordering :DESC}})
      :result
      first))

(defn retrieve-drafts [objective-id]
  (->> (storage/pg-retrieve {:entity :draft :objective-id objective-id}
                           {:sort {:field :_created_at :ordering :DESC}})
       :result 
       (take 50)))
