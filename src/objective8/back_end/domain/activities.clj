(ns objective8.back-end.domain.activities
  (:require [org.httpkit.client :as http]
            [objective8.back-end.storage.storage :as storage]
            [objective8.utils :as utils]
            [objective8.back-end.domain.objectives :as objectives]))

(defn objective->activity [objective]
  {"@context"  "http://www.w3.org/ns/activitystreams"
   "@type"     "Create"
   "published" (:_created_at objective)
   "actor"     {"@type"       "Person"
                "displayName" (:username objective)}
   "object"    {"@type"       "Objective"
                "displayName" (:title objective)
                "content"     (:description objective)
                "url"         (str utils/host-url (:uri objective))}})

(defn question->activity [question]
  (let [objective (objectives/get-objective (:objective-id question))]
    {"@context"  "http://www.w3.org/ns/activitystreams"
     "@type"     "Question"
     "published" (:_created_at question)
     "actor"     {"@type"       "Person"
                  "displayName" (:username question)}
     "object"    {"@type"       "Objective Question"
                  "displayName" (:question question)
                  "url"         (str utils/host-url (:uri question))
                  "object"      {"@type"       "Objective"
                                 "displayName" (:title objective)}}}))

(def entities->activities {:objective objective->activity
                           :question  question->activity})

(defn get-mapping
  "Returns a mapping function between the given entity and an activity"
  [{:keys [entity]}]
  (if-let [entity-to-activity-mapping-fn (get entities->activities entity)]
    entity-to-activity-mapping-fn
    (throw (Exception. (str "No entity mapping for " {:entity entity})))))

(defprotocol ActivityStorage
  (store-activity [this activity]))

(defrecord CoracleActivityStorage [coracle-bearer-token coracle-storage-url]
  ActivityStorage
  (store-activity [this activity-json]
    (http/post coracle-storage-url {:headers {"bearer_token" coracle-bearer-token "Content-Type" "application/activity+json"}
                                    :body activity-json})))

(defn new-coracle-activity-storage [bearer-token url]
  (CoracleActivityStorage. bearer-token url))

(defrecord DBActivityStorage []
  ActivityStorage
  (store-activity [this activity-json]
    (-> activity-json
       (assoc :entity :activity)
        storage/pg-store!)))

(defn store-activity!
  ([activity-storage entity]
   (let [entity-to-activity-mapping-fn (get-mapping entity)]
     (->> entity
          entity-to-activity-mapping-fn
          (store-activity activity-storage))))
  ([entity]
   (store-activity! (DBActivityStorage.) entity)))

(defn retrieve-activities [query]
  (storage/pg-retrieve-activities (assoc query :entity :activity)))

