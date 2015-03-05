(ns objective8.answers
  (:require [objective8.storage.storage :as storage]))

(defn store-answer! [answer]
 (storage/pg-store! (assoc answer :entity :answer)))

(defn retrieve-answers [question-id]
  (:result (storage/pg-retrieve {:entity :answer 
                                 :question-id question-id}
                                {:limit 50})))
