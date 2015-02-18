(ns objective8.api.tokens-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.integration-helpers :as helpers]
            [objective8.storage.storage :as storage]
            [objective8.storage.database :as db]
            [objective8.bearer-tokens :as bearer-tokens]))

(defn db-connection [] (db/connect! db/postgres-spec))

(def app (helpers/test-context))

(facts "Bearer token tests" :integration
       (against-background [(before :contents (db-connection)) (after :facts (helpers/truncate-tables))]
       (fact "api is protected by bearer-tokens"
             ; Temporarily using stub-token-provider so stored tokens not required.  Will be fixed during #47.
             (let [the-token "some-secure-token"
                   the-bearer "objective8.dev"] 
               (storage/pg-store! {:entity :bearer-token :bearer-name the-bearer :bearer-token the-token})
               (p/request app "/api/v1/users"
                          :request-method :post
                          :content-type "application/json"
                          :headers {"api-bearer-token" "some-wrong-token"
                                    "api-bearer-name" "objective8.dev"}
                          :body (json/generate-string {:twitter-id "Twitter_ID"})) 
               => (contains {:response (contains  {:status 401})}))))) 
