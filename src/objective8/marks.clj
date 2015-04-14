(ns objective8.marks
  (:require [objective8.storage.storage :as storage]
            [objective8.storage.uris :as uris]
            [objective8.utils :as utils]))

(defn uri-for-mark-id [mark-id]
  (str "/meta/marks/" mark-id))

(defn mark-data->mark [{:keys [question-uri created-by-uri] :as mark-data}]
  {:entity :mark
   :question-id (:_id (uris/uri->query question-uri))
   :created-by-id (:_id (uris/uri->query created-by-uri))
   :active true})

(defn store-mark! [{:keys [question-uri created-by-uri] :as mark-data}]
  (some-> mark-data
          mark-data->mark
          storage/pg-store!
          (assoc :question-uri question-uri :created-by-uri created-by-uri)
          (utils/update-in-self [:uri] (comp uri-for-mark-id :_id))
          (dissoc :_id :question-id :created-by-id)))
