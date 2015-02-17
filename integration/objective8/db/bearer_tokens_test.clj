(ns objective8.db.bearer_tokens_test
  (:require [midje.sweet :refer :all]
            [objective8.bearer-tokens :as bt]
            [objective8.storage.storage :as s]
            [objective8.storage.database :as db]
            [objective8.integration-helpers :as helpers]))

(facts "Bearer tokens" :integration

       (against-background
         [(before :contents (do (helpers/db-connection)
                                (s/pg-store! {:entity :bearer-token
                                              :bearer-name "mr api"
                                              :bearer-token "12345"})))
          (after :facts (helpers/truncate-tables))]

       (fact "Gets a bearer token from a bearer-name"
             (bt/token-provider "mr api") => "12345") 

       (fact "Returns nil if the token does not exist for a given name"
             (bt/token-provider "some other guy") => nil))) 
