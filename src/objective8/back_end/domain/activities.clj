(ns objective8.back-end.domain.activities
  (:require [objective8.back-end.storage.storage :as storage]
            [objective8.utils :as utils]))

(defn objective->activity [objective]
  {"@context" "http://www.w3.org/ns/activitystreams"
   "@type" "Create"
   "published" (:_created_at objective)
   "actor" {"@type" "Person"
            "displayName" (:username objective)}
   "object" {"@type" "Objective"
             "displayName" (:title objective)
             "content" (:description objective)
             "url" (str utils/host-url  (:uri objective))}})

(defn store-activity! [objective]
  (-> (objective->activity objective)
      (assoc :entity :activity)
      storage/pg-store!))

(defn retrieve-activities []
  (:result (storage/pg-retrieve {:entity :activity} {:sort {:field :_created_at
                                                            :ordering :DESC}})))
