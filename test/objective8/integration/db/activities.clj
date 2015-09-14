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
                                                                       "content" "objective description"
                                                                       "url" stored-objective-url}}))

(fact "activities can be retrieved in reverse chronological order"
      (let [first-stored-activity (sh/store-an-activity)
            latest-stored-activity (sh/store-an-activity)]
      (activities/retrieve-activities {}) => [latest-stored-activity first-stored-activity]))

(fact "can retrieve n number of entries"
      (doall (repeatedly 10 sh/store-an-activity))
      (count (activities/retrieve-activities {})) => 10
      (count (activities/retrieve-activities {:limit 5})) => 5
      (count (activities/retrieve-activities {:limit 3})) => 3)

(fact "can provide offset for paging"
      (let [a1 (sh/store-an-activity)
            a2 (sh/store-an-activity)
            a3 (sh/store-an-activity)
            a4 (sh/store-an-activity)]
        (activities/retrieve-activities {}) => [a4 a3 a2 a1]
        (activities/retrieve-activities {:limit 2}) => [a4 a3]
        (activities/retrieve-activities {:limit 2 :offset 1}) => [a3 a2]
        (activities/retrieve-activities {:limit 2 :offset 2}) => [a2 a1]
        (activities/retrieve-activities {:limit 2 :offset 3}) => [a1]
        (activities/retrieve-activities {:limit 2 :offset 4}) => []
        (activities/retrieve-activities {:limit 3 :offset 1}) => [a3 a2 a1]
        (activities/retrieve-activities {:limit 2 :offset 9}) => []))

(fact "activities can be retrieved by from and to timestamps"
      (let [a1 (sh/store-an-activity)
            a2 (sh/store-an-activity)
            a3 (sh/store-an-activity)
            a4 (sh/store-an-activity)
            a2-timestamp (get a2 "published")
            a4-timestamp (get a4 "published")]
        (activities/retrieve-activities {:from-date a2-timestamp}) => [a4 a3]
        (activities/retrieve-activities {:to-date a2-timestamp}) => [a1]
        (activities/retrieve-activities {:from-date a2-timestamp :to-date a4-timestamp}) => [a3]
        (activities/retrieve-activities {}) => [a4 a3 a2 a1]))
