(ns objective8.objectives
  (:require [objective8.storage.mappings :as mappings]
            [objective8.storage.storage :as storage]
            [objective8.utils :as utils]))

(defn open? [objective]
  (when (= "open" (:status objective)) objective))

(defn in-drafting? [objective]
  (when (= "drafting" (:status objective)) objective))

(defn uri-for-objective [{:keys [_id] :as objective}]
  (str "/objectives/" _id))

(defn store-objective! [objective-data]
  (some-> objective-data
          (assoc :entity :objective
                 :status "open")
          storage/pg-store!
          (utils/update-in-self [:uri] uri-for-objective)
          (dissoc :global-id)))

(defn get-objective-as-signed-in-user [objective-id user-id]
  (some-> (storage/pg-get-objective-as-signed-in-user objective-id user-id)
          (dissoc :global-id)
          (utils/update-in-self [:uri] uri-for-objective)))

(defn get-objective [objective-id]
  (some-> (storage/pg-get-objective objective-id)
          first
          (dissoc :global-id)
          (utils/update-in-self [:uri] uri-for-objective)))

(defn retrieve-objectives []
  (->> (storage/pg-retrieve {:entity :objective}
                            {:limit 50
                             :sort {:field :_created_at
                                    :ordering :DESC}})
       :result
       (map #(dissoc % :global-id))
       (map #(utils/update-in-self % [:uri] uri-for-objective))))

(defn get-objectives-as-signed-in-user [user-id]
  (->> (storage/pg-get-objectives-as-signed-in-user user-id) 
       (map #(dissoc % :global-id))
       (map #(utils/update-in-self % [:uri] uri-for-objective))))

(defn retrieve-starred-objectives [user-id]
  (->> (storage/pg-retrieve-starred-objectives user-id)
       (map #(dissoc % :global-id))
       (map #(utils/update-in-self % [:uri] uri-for-objective))))

(defn get-objectives [{:keys [signed-in-id filters] :as query}]
  (if-let [signed-in-id (:signed-in-id query)]
    (if (:starred filters)
      (retrieve-starred-objectives signed-in-id)
      (get-objectives-as-signed-in-user signed-in-id))
    (retrieve-objectives)))

(defn retrieve-objectives-due-for-drafting []
  (->> (storage/pg-retrieve {:entity :objective 
                             :end-date ['< (mappings/iso-date-time->sql-time (utils/current-time))]
                             :status (mappings/string->postgres-type "objective_status" "open")}) 
       :result
       (map #(dissoc % :global-id))
       (map #(utils/update-in-self % [:uri] uri-for-objective))))

(defn get-objectives-owned-by-user-id [user-id]
 (->> (storage/pg-retrieve {:entity :objective
                            :created-by-id user-id})
      :result
      (map #(dissoc % :global-id))))
