(ns objective8.integration.db.activities
  (:require [midje.sweet :refer :all]
            [org.httpkit.client :as http]
            [objective8.utils :as utils]
            [objective8.integration.storage-helpers :as sh]
            [objective8.back-end.domain.activities :as activities]
            [objective8.back-end.domain.objectives :as objectives]
            [objective8.back-end.domain.questions :as questions]
            [cheshire.core :as json]))

(defrecord StubActivityStorage [a]
  activities/ActivityStorage
  (store-activity [this activity]
    (swap! a conj activity)
    activity))

(defn create-stub-activity-storage [atom]
  (StubActivityStorage. atom))

(facts "about store-activity!"
       (fact "stores activities of type 'objective'"
             (let [activities-atom (atom [])
                   activity-storage (create-stub-activity-storage activities-atom)
                   {user-id :_id username :username} (sh/store-a-user)
                   objective-data {:created-by-id user-id
                                   :description   "objective description"
                                   :title         "objective title"}
                   stored-objective (objectives/store-objective! objective-data)
                   stored-objective-timestamp (:_created_at stored-objective)
                   stored-objective-url (str utils/host-url (:uri stored-objective))
                   retrieved-objective (objectives/get-objective (:_id stored-objective))
                   expected-activity {"@context"  "http://www.w3.org/ns/activitystreams"
                                      "type"      "Create"
                                      "published" stored-objective-timestamp
                                      "actor"     {"type" "Person"
                                                   "name" username}
                                      "object"    {"type"    "Objective"
                                                   "name"    "objective title"
                                                   "content" "objective description"
                                                   "url"     stored-objective-url}}]
               (activities/store-activity! activity-storage retrieved-objective) => expected-activity
               @activities-atom => [expected-activity]))

       (fact "stores activities of type 'question'"
             (let [activities-atom (atom [])
                   activity-storage (create-stub-activity-storage activities-atom)
                   {objective-user-id :_id} (sh/store-a-user)
                   {question-user-id :_id question-username :username} (sh/store-a-user)
                   objective-data {:created-by-id objective-user-id
                                   :description   "objective description"
                                   :title         "objective title"}
                   stored-objective (objectives/store-objective! objective-data)
                   stored-objective-url (str utils/host-url (:uri stored-objective))
                   question-data {:created-by-id question-user-id
                                  :objective-id  (:_id stored-objective)
                                  :question      "Question content"}
                   stored-question (questions/store-question! question-data)
                   stored-question-timestamp (:_created_at stored-question)
                   stored-question-url (str utils/host-url (:uri stored-question))
                   retrieved-question (questions/get-question (:uri stored-question))
                   expected-activity {"@context"  "http://www.w3.org/ns/activitystreams"
                                      "type"      "Question"
                                      "published" stored-question-timestamp
                                      "actor"     {"type" "Person"
                                                   "name" question-username}
                                      "object"    {"type"
                                                          "Objective Question"
                                                   "name" "Question content"
                                                   "url"  stored-question-url}
                                      "target"    {"type" "Objective"
                                                   "name" "objective title"
                                                   "url"  stored-objective-url}}]
               (activities/store-activity! activity-storage retrieved-question) => expected-activity
               @activities-atom => [expected-activity]))

       (fact "throws exception if no entity key is present"
             (activities/store-activity! {}) => (throws Exception "No entity mapping for {:entity nil}"))
       (fact "throws exception if entity key is not recognised in mappings"
             (activities/store-activity! {:entity :unknown}) => (throws Exception "No entity mapping for {:entity :unknown}")))

(facts "about storing activities to coracle"
       (let [bearer-token "a-bearer-token"
             coracle-url "coracle-post-url"
             body "an-activity"
             coracle (activities/new-coracle-activity-storage bearer-token coracle-url)]
         (activities/store-activity! coracle body) => body
         (provided
           (activities/get-mapping anything) => identity
           (http/request {:method :post :headers {"bearer-token" bearer-token "Content-Type" "application/activity+json"}
                          :url    coracle-url :body (json/generate-string body)} nil) => (atom {:status 201}) :times 1)))

(facts "failure cases when storing activities to coracle"
       (fact "silently logs when coracle returns error"
             (let [coracle (activities/new-coracle-activity-storage "bearer-token" "url")]
               (activities/store-activity! coracle "some-activity") => nil
               (provided
                 (activities/get-mapping anything) => identity
                 (http/request anything anything) =throws=> (Exception. "ERROR posting activity"))))
       (fact "silently logs when coracle returns non-201 code"
             (let [coracle (activities/new-coracle-activity-storage "bearer-token" "url")]
               (activities/store-activity! coracle "some-activity") => nil
               (provided
                 (activities/get-mapping anything) => identity
                 (http/request anything anything) => (atom {:status 401})))))