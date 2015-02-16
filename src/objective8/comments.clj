(ns objective8.comments
  (:require [objective8.storage.storage :as storage]))

(defn store-comment! [comment]
 (storage/pg-store! (assoc comment :entity :comment)))


(defn retrieve-comments [objective-id]
  (let [{result :result} (storage/pg-retrieve {:entity :comment :objective-id objective-id}
                                              {:limit 50})]
    (map #(dissoc % :entity) result)))
