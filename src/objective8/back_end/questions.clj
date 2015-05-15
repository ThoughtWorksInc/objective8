(ns objective8.back-end.questions
  (:require [objective8.back-end.objectives :as objectives]
            [objective8.back-end.storage.uris :as uris]
            [objective8.utils :as utils]
            [objective8.back-end.storage.storage :as storage]))

(defn store-question! [question]
  (some-> (storage/pg-store! (assoc question :entity :question))
          (utils/update-in-self [:uri] uris/question->uri)))

(defn create-question [{objective-id :objective-id :as question}]
  (when (objectives/open? (objectives/get-objective objective-id))
    (store-question! question)))

(defn get-questions-for-objective [objective-uri]
  (let [{entity :entity objective-id :_id :as query} (uris/uri->query objective-uri)]
    (when (= :objective entity)
      (->> (storage/pg-retrieve-questions-for-objective objective-id)
           (map #(utils/update-in-self % [:uri] uris/question->uri))))))

(defn get-questions-for-objective-by-most-answered [objective-uri]
  (let [{entity :entity objective-id :_id :as query} (uris/uri->query objective-uri)]
    (when (= :objective entity)
      (->> (storage/pg-retrieve-questions-for-objective-by-most-answered {:entity entity :objective_id objective-id})
           (map #(utils/update-in-self % [:uri] uris/question->uri))))))

(defn get-question [question-uri]
  (let [query (uris/uri->query question-uri)]
    (some-> (storage/pg-retrieve-question-by-query-map query)
            (utils/update-in-self [:uri] uris/question->uri))))
