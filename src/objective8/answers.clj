(ns objective8.answers
  (:require [objective8.storage.storage :as storage]
            [objective8.objectives :as objectives]
            [objective8.utils :as utils]))

(defn uri-for-answer [answer]
  (str "/objectives/" (:objective-id answer)
       "/questions/" (:question-id answer)
       "/answers/" (:_id answer)))

(defn store-answer! [answer-data]
  (some-> answer-data
          (assoc :entity :answer)
          storage/pg-store!
          (dissoc :global-id)
          (utils/update-in-self [:uri] uri-for-answer)))

(defn create-answer! [{obj-id :objective-id :as answer}]
  (when (objectives/open? (objectives/retrieve-objective obj-id))
    (store-answer! answer)))

(defn get-answers [question-id]
  (->> (storage/pg-retrieve-answers-with-votes-for-question question-id)
       (map #(dissoc % :global-id))
       (map #(utils/update-in-self % [:uri] uri-for-answer))))

