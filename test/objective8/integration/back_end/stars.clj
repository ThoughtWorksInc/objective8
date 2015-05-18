(ns objective8.integration.back-end.stars
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.middleware :as m]
            [objective8.integration.storage-helpers :as sh]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.back-end.storage.domain.stars :as stars]))

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
                    objective-uri (str "/objectives/" objective-id)
                    data {:objective-uri objective-uri
                          :created-by-id user-id}
                    {response :response} (p/request app "/api/v1/meta/stars"
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string data))]
                (:status response) => 201
                (:body response) => (helpers/json-contains {:_id integer?
                                                            :objective-id objective-id
                                                            :created-by-id user-id
                                                            :active true })
                (:headers response) => (helpers/location-contains "/api/v1/meta/stars/")))

        (tabular
         (fact "posting a star to an objective that has been starred by the posting user toggles the star state"
               (let [{user-id :_id :as user} (sh/store-a-user)
                     {objective-id :_id :as starred-objective} (sh/store-an-open-objective)
                     _ (sh/store-a-star {:user user :objective starred-objective :active ?active})

                     objective-uri (str "/objectives/" objective-id)
                     data {:objective-uri objective-uri
                           :created-by-id user-id}
                     {response :response} (p/request app "/api/v1/meta/stars"
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body (json/generate-string data))]
                 (:status response) => 201
                 (:body response) => (helpers/json-contains {:_id integer?
                                                             :objective-id objective-id
                                                             :created-by-id user-id
                                                             :active (not ?active)})
                 (:headers response) => (helpers/location-contains "/api/v1/meta/stars/")))

         ?active true false)

        (fact "returns 404 if the objective to be starred doesn't exist"
              (let [{user-id :_id} (sh/store-a-user)
                    star-data {:objective-uri "any" 
                               :created-by-id user-id}
                    {response :response} (p/request app "/api/v1/meta/stars"
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string star-data))]

                (:status response) => 404
                (:body response) => (helpers/json-contains {:reason "Objective does not exist"})))))
