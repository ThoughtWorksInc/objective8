(ns objective8.answers
  (:require [objective8.storage.storage :as storage]
            [objective8.objectives :refer [open?] :as objectives]))

(defn store-answer! [answer]
 (storage/pg-store! (assoc answer :entity :answer)))

(defn create-answer! [{obj-id :objective-id :as answer}]
  (when (open? (objectives/retrieve-objective obj-id))
    (store-answer! answer)))

(defn retrieve-answers [question-id]
  (:result (storage/pg-retrieve {:entity :answer 
                                 :question-id question-id}
                                {:limit 50})))
