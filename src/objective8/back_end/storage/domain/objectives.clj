(ns objective8.back-end.storage.domain.objectives
  (:require [objective8.back-end.storage.mappings :as mappings]
            [objective8.back-end.storage.storage :as storage]
            [objective8.back-end.storage.uris :as uris]
            [objective8.config :as config]
            [objective8.utils :as utils]))

(defn open? [objective]
  (if config/two-phase?
    (= "open" (:status objective)) 
    true))

(defn in-drafting? [objective]
  (not (and (open? objective)
            config/two-phase?)))

(defn uri-for-objective [{:keys [_id] :as objective}]
  (str "/objectives/" _id))

(defn store-objective! [objective-data]
  (some-> objective-data
          (assoc :entity :objective
                 :status "open"
                 :removed-by-admin false)
          storage/pg-store!
          (utils/update-in-self [:uri] uri-for-objective)
          (dissoc :global-id)))

(defn include-removed-objective? [retrieved-objectives include?]
  (if include?
    retrieved-objectives
    (remove :removed-by-admin retrieved-objectives)))

(defn get-objective-as-signed-in-user [objective-id user-id]
  (some-> (storage/pg-get-objective-as-signed-in-user objective-id user-id)
          (dissoc :global-id)
          (utils/update-in-self [:uri] uri-for-objective)))

(defn get-objective 
  ([objective-id]
   (get-objective objective-id false)) 
  ([objective-id include-removed?]
   (some-> (storage/pg-get-objective objective-id)
           (include-removed-objective? include-removed?)
           first
           (dissoc :global-id)
           (utils/update-in-self [:uri] uri-for-objective))))

(defn retrieve-objectives 
  ([]
   (retrieve-objectives false)) 
  ([include-removed?] 
   (let [query-map (cond-> {:entity :objective} 
                     (not include-removed?) (assoc :removed-by-admin false))]
     (->> (storage/pg-retrieve query-map
                               {:limit 50
                                :sort {:field :_created_at
                                       :ordering :DESC}})
          :result
          (map #(dissoc % :global-id))
          (map #(utils/update-in-self % [:uri] uri-for-objective))))))

(defn get-objectives-as-signed-in-user [user-id]
  (->> (storage/pg-get-objectives-as-signed-in-user user-id) 
       (map #(dissoc % :global-id))
       (map #(utils/update-in-self % [:uri] uri-for-objective))))

(defn retrieve-starred-objectives [user-id]
  (->> (storage/pg-retrieve-starred-objectives user-id)
       (map #(dissoc % :global-id))
       (map #(utils/update-in-self % [:uri] uri-for-objective))))

(defn get-objectives-for-writer [user-id]
  (->> (storage/pg-get-objectives-for-writer user-id)
       (map #(dissoc % :global-id))
       (map #(utils/update-in-self % [:uri] uri-for-objective))))

(defn get-objectives [{:keys [signed-in-id filters include-removed?] :as query}]
  (if-let [signed-in-id (:signed-in-id query)]
    (if (:starred filters)
      (retrieve-starred-objectives signed-in-id)
      (get-objectives-as-signed-in-user signed-in-id))
    (retrieve-objectives include-removed?)))

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

(defn admin-remove-objective! [objective]
  (some-> (storage/pg-update-objective! objective :removed-by-admin true)
          (utils/update-in-self [:uri] uri-for-objective)
          (dissoc :global-id)))
