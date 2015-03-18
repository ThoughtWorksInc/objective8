(ns objective8.up-down-votes
  (:require [objective8.storage.storage :as storage]))

(defn store-vote! [vote-data]
  (storage/pg-store! (assoc vote-data :entity :up-down-vote)))

(defn get-vote [global-id user-id]
  (first (:result (storage/pg-retrieve {:entity :up-down-vote
                                        :global-id global-id
                                        :user-id user-id}))))
