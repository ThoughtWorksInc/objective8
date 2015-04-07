(ns objective8.integration.api.stars
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.middleware :as m]
            [objective8.integration.storage-helpers :as sh]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.stars :as stars])
  )

(def app (helpers/test-context))

(facts "POST /api/v1/meta/stars"
       (against-background
         (m/valid-credentials? anything anything anything) => true)
       (against-background 
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables))) 
          (after :facts (helpers/truncate-tables))]
        (fact "the posted star is stored"
              (let [{user-id :created-by-id objective-id :_id} (sh/store-an-open-objective)
                    data {:objective-id objective-id
                          :created-by-id user-id }
                    {response :response} (p/request app "/api/v1/meta/stars"
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string data))]
                (:status response) => 201
                (:body response) => (helpers/json-contains {:_id integer?
                                                            :objective-id objective-id 
                                                            :created-by-id user-id
                                                            :active true })
                (:headers response) => (helpers/location-contains "/api/v1/meta/stars/")))))

