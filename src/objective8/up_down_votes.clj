(ns objective8.up-down-votes
  (:require [objective8.storage.storage :as storage]))

(defn store-vote! [vote-data]
  (storage/pg-store! (assoc vote-data :entity :up-down-vote :active true)))

(defn nullify-vote! [previous-vote]
  (storage/pg-deactivate-up-down-vote! previous-vote))

(defn update-vote! [previous-vote new-vote-data]
  (nullify-vote! previous-vote)
  (store-vote! new-vote-data))

(defn get-active-vote [ueid user-id]
  (first (:result (storage/pg-retrieve {:entity :up-down-vote
                                        :ueid ueid
                                        :user-id user-id
                                        :active true}))))
