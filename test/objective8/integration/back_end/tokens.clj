(ns objective8.integration.back-end.tokens
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.back-end.storage.storage :as storage]
            [objective8.back-end.storage.database :as db]
            [objective8.back-end.domain.bearer-tokens :as bearer-tokens]))

(def app (helpers/api-context))
(def the-token "token")
(def some-wrong-token "wrong-token")
(def the-bearer "bearer")
(def bearer-token-map {:entity :bearer-token :bearer-name the-bearer :bearer-token the-token})

(facts "Bearer token tests"
       (against-background [(before :contents (do (helpers/db-connection) 
                                                  (helpers/truncate-tables))) 
                            (after :facts (helpers/truncate-tables))]

                           (fact "can't access protected api resource without valid bearer-token"
                                 (storage/pg-store! bearer-token-map) 
                                 (p/request app (utils/api-path-for :api/post-user-profile)
                                            :request-method :post
                                            :content-type "application/json"
                                            :headers {"api-bearer-token" some-wrong-token
                                                      "api-bearer-name" the-bearer}
                                            :body (json/generate-string {:auth-provider-user-id "twitter-TWITTER_ID"
                                                                         :username "username"}))
                                 => (contains {:response (contains  {:status 401})}))

                           (fact "can access protected api resource with valid bearer-token"
                                 (storage/pg-store! bearer-token-map) 
                                 (p/request app (utils/api-path-for :api/post-user-profile)
                                            :request-method :post
                                            :content-type "application/json"
                                            :headers {"api-bearer-token" the-token
                                                      "api-bearer-name" the-bearer}
                                            :body (json/generate-string {:auth-provider-user-id "twitter-TWITTER_ID"
                                                                         :username "username"}))
                                 => (contains {:response (contains  {:status 201})}))))
