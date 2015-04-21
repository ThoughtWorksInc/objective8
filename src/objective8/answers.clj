(ns objective8.answers
  (:require [objective8.storage.storage :as storage]
            [objective8.objectives :as objectives]
            [objective8.storage.uris :as uris]
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

(defn create-answer! [{objective-id :objective-id :as answer}]
  (when (objectives/open? (objectives/retrieve-objective objective-id))
    (store-answer! answer)))

(defn get-answers [question-uri]
  (let [query (-> (uris/uri->query question-uri)
                  (utils/ressoc :_id :question-id))]
    (->> (storage/pg-retrieve-answers-with-votes query)
         (map #(dissoc % :global-id))
         (map #(utils/update-in-self % [:uri] uri-for-answer)))))

