(ns objective8.back-end.up-down-votes
  (:require [objective8.storage.storage :as storage]
            [objective8.utils :as utils]))

(defn store-vote! [{:keys [global-id] :as entity-to-vote-on}
                   {:keys [vote-on-uri] :as vote-data}]
  (some-> vote-data
          (utils/select-all-or-nothing [:vote-type :created-by-id])
          (assoc :entity :up-down-vote
                 :global-id global-id)
          storage/pg-store!
          (dissoc :global-id)
          (assoc :vote-on-uri vote-on-uri)))

(defn get-vote [global-id created-by-id]
  (first (:result (storage/pg-retrieve {:entity :up-down-vote
                                        :global-id global-id
                                        :created-by-id created-by-id}))))
