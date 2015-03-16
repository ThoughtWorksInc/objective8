(ns objective8.integration.db.bearer_tokens
  (:require [midje.sweet :refer :all]
            [objective8.bearer-tokens :as bt]
            [objective8.storage.storage :as s]
            [objective8.storage.database :as db]
            [objective8.integration-helpers :as helpers]))

(def bearer-name "mr-api")
(def bearer-token "12345")
(def bearer-token-map
  {:entity :bearer-token
   :bearer-name bearer-name 
   :bearer-token bearer-token})

(facts "Bearer tokens" :integration

       (against-background
         [(before :contents (do (helpers/db-connection)
                                (s/pg-store! bearer-token-map)))
          (after :facts (helpers/truncate-tables))]

         (fact "Gets a bearer token from a bearer-name"
               (bt/token-provider bearer-name) => bearer-token) 

         (fact "Returns nil if the token does not exist for a given name"
               (bt/token-provider "some other guy") => nil))) 
