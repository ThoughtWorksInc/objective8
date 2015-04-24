(ns objective8.writer-notes
  (:require [objective8.storage.storage :as storage]
            [objective8.utils :as utils]))

(defn replace-note-on-id [note note-on-uri]
  (-> note
      (assoc :note-on-uri note-on-uri)
      (dissoc :note-on-id)))

(defn uri-for-note [note]
  (str "/notes/" (:_id note)))

(defn store-note-for! [entity-to-note-on {:keys [note-on-uri] :as note-data}]
  (when-let [{:keys [objective-id _id global-id]} entity-to-note-on]
    (some-> note-data
            (utils/select-all-or-nothing [:note :created-by-id])
            (assoc :entity :note
                   :note-on-id global-id
                   :objective-id (or objective-id _id))
            (dissoc :note-on-uri)
            storage/pg-store!
            (dissoc :global-id)
            (utils/update-in-self [:uri] uri-for-note)
            (replace-note-on-id note-on-uri))))

(defn retrieve-note [entity-uri])
