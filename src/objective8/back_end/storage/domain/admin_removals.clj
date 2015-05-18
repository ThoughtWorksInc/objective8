(ns objective8.back-end.storage.domain.admin-removals
  (:require [objective8.back-end.storage.uris :as uris]
            [objective8.back-end.storage.storage :as storage]
            [objective8.utils :as utils]))

(defn uri-for-admin-removal [admin-removal]
  (str "/admin-removals/" (:_id admin-removal)))

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

(defn- ids->uris [{:keys [removed-by-id] :as admin-removal}]
  (-> admin-removal
      (utils/update-in-self [:uri] uri-for-admin-removal)
      (assoc :removed-by-uri (uris/user-id->uri removed-by-id))
      (dissoc :_id :removed-by-id)))

(defn get-admin-removals []
  (->> (storage/pg-retrieve {:entity :admin-removal}
                            {:limit 50
                             :sort {:field :_created_at
                                    :ordering :DESC}})
       :result
       (map ids->uris)))
