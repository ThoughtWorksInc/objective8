(ns objective8.integration.db.activities
  (:require [midje.sweet :refer :all]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]
            [objective8.back-end.domain.activities :as activities]
            [objective8.back-end.domain.objectives :as objectives]))

(background
  [(before :contents (do (ih/db-connection)
                         (ih/truncate-tables)))
   (after :facts (ih/truncate-tables))])

(fact "an activity can be stored"
      (let [{user-id :_id username :username} (sh/store-a-user)
            objective-data {:created-by-id user-id
                            :description "objective description"
                            :title "objective title"}
            stored-objective (objectives/store-objective! objective-data)
            stored-objective-timestamp (:_created_at stored-objective)
            stored-objective-url (str utils/host-url (:uri stored-objective))
            retrieved-objective (objectives/get-objective (:_id stored-objective))]
        (activities/store-activity! retrieved-objective) => {"@context" "http://www.w3.org/ns/activitystreams"
                                                             "@type" "Create"
                                                             "published" stored-objective-timestamp
                                                             "actor" {"@type" "Person"
                                                                      "displayName" username}
                                                             "object" {"@type" "Objective"
                                                                       "displayName" "objective title"
                                                                       "description" "objective description"
                                                                       "url" stored-objective-url}}))

(fact "activities can be retrieved"
      (let [stored-activity-1 (-> (sh/store-an-objective)
                                      :_id
                                      objectives/get-objective
                                      activities/store-activity!)
            stored-activity-2 (-> (sh/store-an-objective)
                                  :_id
                                  objectives/get-objective
                                  activities/store-activity!)]
      (activities/retrieve-activities) => [stored-activity-1 stored-activity-2]))
