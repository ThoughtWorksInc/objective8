(ns objective8.api.tokens-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [objective8.integration-helpers :as helpers]
            [objective8.storage.storage :as storage]
            [objective8.storage.database :as db]
            [objective8.bearer-tokens :as bearer-tokens]))

(defn db-connection [] (db/connect! db/postgres-spec))


(def app (helpers/test-context))

(defn store-user-and-objective! []
 (let [stored-user (storage/pg-store! {:entity :user :twitter-id "twitter-id"})
       user-id (:_id stored-user) 
       stored-objective (storage/pg-store! {:entity :objective :created-by-id user-id :end-date (new java.util.Date)})]
   {:user-id user-id :objective-id (:_id stored-objective)}))

(facts "Bearer token tests" :integration
       (against-background [(before :contents (db-connection)) (after :facts (helpers/truncate-tables))]
       (future-fact "api is protected by bearer-tokens"
             (let [the-token "some-secure-token"
                   the-bearer "objective8.dev" 
              db-ids (store-user-and-objective!)] 
               (storage/pg-store! {:entity :bearer-token :bearer-name the-bearer :bearer-token the-token})
               (p/request app (str "/api/v1/objectives/" (:objective-id db-ids))
                          :headers {"api-bearer-token" "some-wrong-token"
                                    "api-bearer-name" "objective8.dev"}) => (contains {:response (contains  {:status 401})}))))) 
