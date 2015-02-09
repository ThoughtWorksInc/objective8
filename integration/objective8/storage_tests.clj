(ns objective8.storage-tests
  (:require [midje.sweet :refer :all]
            [objective8.storage.storage :as storage]
            [objective8.storage.database :as db]))


(def db-connection (db/connect! db/postgres-spec))

(fact "a user entity can be stored in the database"
      (let [user {:entity :user :twitter-id "the-twitter-id" :foo "bar"}
            store-result (storage/pg-store! user)
            retrieve-result (storage/pg-retrieve {:entity :user :_id (:_id store-result)})]
        (first (:result retrieve-result))) => (contains {:twitter-id "the-twitter-id" :foo "bar"}))

(fact "an objective entity can be stored in the database"
      (let [objective {:entity :objective
                       :created-by-id 1
                       :end-date "2015-01-01T00:00:00Z"
                       :description "description"
                       :goals "goals"
                       :title "title"}
            store-result (storage/pg-store! objective)
            retrieve-result (storage/pg-retrieve {:entity :objective :_id (:_id store-result)})]
        (first (:result retrieve-result))) => (contains {:created-by-id 1
                                                         :end-date "2015-01-01T00:00:00Z"
                                                         :title "title"}))
