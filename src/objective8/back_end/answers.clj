(ns objective8.back-end.answers
  (:require [objective8.storage.storage :as storage]
            [objective8.back-end.objectives :as objectives]
            [objective8.back-end.writer-notes :as notes]
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
  (when (objectives/open? (objectives/get-objective objective-id))
    (store-answer! answer)))

(defn get-answers [question-uri query-params]
  (let [query (-> (uris/uri->query question-uri)
                  (utils/ressoc :_id :question-id)
                  (assoc :sorted-by (:sorted-by query-params))
                  (assoc :filter-type (:filter-type query-params)))]
    (->> (storage/pg-retrieve-answers query)
         (map #(dissoc % :global-id))
         (map #(utils/update-in-self % [:uri] uri-for-answer)))))
