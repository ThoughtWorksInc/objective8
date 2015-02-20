(ns objective8.questions
  (:require [objective8.storage.storage :as storage]))

(defn store-question! [question]
 (storage/pg-store! (assoc question :entity :question)))

(defn retrieve-question [question-id]
  (let [{result :result} (storage/pg-retrieve {:entity :question :_id question-id})]
  (dissoc (first result) :entity)))

(defn retrieve-questions [objective-id]
  (let [{result :result} (storage/pg-retrieve {:entity :question :objective-id objective-id}
                                              {:limit 50})]
    (map #(dissoc % :entity) result)))

