(ns objective8.objectives
  (:require [objective8.storage.storage :as storage]))

(def open? (complement :drafting-started))

(defn in-drafting? [objective]
  (when (:drafting-started objective) objective))

(defn uri-for-objective [{:keys [_id] :as objective}]
  (str "/objectives/" _id))

(defn update-in-self [m key-route update-fn]
  (assoc-in m key-route (update-fn m)))

(defn store-objective! [objective-data]
  (some-> objective-data
          (assoc :entity :objective)
          storage/pg-store!
          (update-in-self [:uri] uri-for-objective)
;          (dissoc :global-id)
          ))

(defn retrieve-objective [objective-id]
  (some-> (storage/pg-retrieve {:entity :objective :_id objective-id})
          :result
          first
          (update-in-self [:uri] uri-for-objective)))

(defn retrieve-objectives []
  (->> (storage/pg-retrieve {:entity :objective}
                            {:limit 50
                             :sort {:field :_created_at
                                    :ordering :DESC}})
       :result
       (map #(update-in-self % [:uri] uri-for-objective))))
