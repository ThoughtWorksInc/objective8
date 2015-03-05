(ns objective8.questions
  (:require [objective8.objectives :as objectives :refer [open?]]
            [objective8.storage.storage :as storage]))

(defn create-question [{objective-id :objective-id :as question}]
  (when (open? (objectives/retrieve-objective objective-id))
    (store-question! question)))

(defn store-question! [question]
 (storage/pg-store! (assoc question :entity :question)))

(defn retrieve-question [question-id]
  (let [{result :result} (storage/pg-retrieve {:entity :question :_id question-id})]
  (dissoc (first result) :entity)))

(defn retrieve-questions [objective-id]
  (let [{result :result} (storage/pg-retrieve {:entity :question :objective-id objective-id}
                                              {:limit 50})]
    (map #(dissoc % :entity) result)))

