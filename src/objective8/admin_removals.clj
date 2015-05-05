(ns objective8.admin-removals
  (:require [objective8.storage.uris :as uris]
            [objective8.storage.storage :as storage]
            [objective8.utils :as utils]))

(defn uri-for-admin-removal [admin-removal]
  (str "/meta/admin-removals/" (:_id admin-removal)))

(defn store-admin-removal! [{:keys [removed-by-uri] :as admin-removal-data}]
 (when-let [{removed-by-id :_id entity :entity} (uris/uri->query removed-by-uri)]
   (when (= :user entity)
     (some-> admin-removal-data
             (utils/select-all-or-nothing [:removal-uri])
             (assoc :entity :admin-removal
                    :removed-by-id removed-by-id)
             (storage/pg-store!)
             (assoc :removed-by-uri removed-by-uri)
             (utils/update-in-self [:uri] uri-for-admin-removal)
             (dissoc :_id :removed-by-id)))))
