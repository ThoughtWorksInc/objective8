(ns objective8.back-end.marks
  (:require [objective8.storage.storage :as storage]
            [objective8.storage.uris :as uris]
            [objective8.utils :as utils]))

(defn uri-for-mark [mark]
  (str "/meta/marks/" (:_id mark)))

(defn created-by-uri-for-mark [mark]
  (str "/users/" (:created-by-id mark)))

(defn mark-data->mark [{:keys [question-uri created-by-uri active] :as mark-data}]
  {:entity :mark
   :question-id (:_id (uris/uri->query question-uri))
   :created-by-id (:_id (uris/uri->query created-by-uri))
   :active active})

(defn store-mark! [{:keys [question-uri created-by-uri] :as mark-data}]
  (some-> mark-data
          mark-data->mark
          storage/pg-store!
          (assoc :question-uri question-uri :created-by-uri created-by-uri)
          (utils/update-in-self [:uri] uri-for-mark)
          (dissoc :_id :question-id :created-by-id)))

(defn get-mark-for-question [question-uri]
  (let [question-id (:_id (uris/uri->query question-uri))]
    (some-> (storage/pg-retrieve {:entity :mark :question-id question-id}
                                 {:sort {:field :_created_at
                                         :ordering :DESC}})
            :result
            first
            (assoc :question-uri question-uri)
            (utils/update-in-self [:created-by-uri] created-by-uri-for-mark)
            (utils/update-in-self [:uri] uri-for-mark)
            (dissoc :_id :question-id :created-by-id))))
