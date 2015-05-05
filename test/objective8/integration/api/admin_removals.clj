(ns objective8.integration.api.admin-removals 
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.middleware :as m]))

(def app (helpers/test-context))

(facts "POST /api/v1/meta/admin-removals"
       (against-background
        (m/valid-credentials? anything anything anything) => true)
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]
        (fact "the posted admin removal is stored"
              (let [admin-uri (->> (sh/store-an-admin)
                                  :_id
                                  (str "/users/"))
                    objective-uri (->> (sh/store-an-open-objective)
                                       :_id
                                       (str "/objectives/"))
                    removal-data {:removal-uri objective-uri
                                  :removed-by-uri admin-uri}
                    {response :response} (p/request app "/api/v1/meta/admin-removals"
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string removal-data))]
                (:status response) => 201
                (:body response) => (helpers/json-contains {:uri (contains "/meta/admin-removals/")
                                                            :removal-uri objective-uri
                                                            :removed-by-uri admin-uri})
                (:headers response) => (helpers/location-contains "/api/v1/meta/admin-removals/")))

        (fact "returns 404 when entity to be removed doesn't exist"
              (let [admin-uri (->> (sh/store-an-admin)
                                  :_id
                                  (str "/users/"))
                    removal-data {:removal-uri "/non-existent-objective"
                                  :removed-by-uri admin-uri}
                    {response :response} (p/request app "/api/v1/meta/admin-removals"
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string removal-data))]
                (:status response) => 404
                (:body response) => (helpers/json-contains {:reason "Entity with that uri does not exist"})))
         
         (fact "removal is not posted if the user is not an admin"
               (let [user-uri (->> (sh/store-a-user)
                                   :_id
                                   (str "/users/"))
                     objective-uri (->> (sh/store-an-open-objective)
                                        :_id
                                        (str "/objectives/"))
                     removal-data {:removal-uri objective-uri
                                   :removed-by-uri user-uri}
                     {response :response} (p/request app "/api/v1/meta/admin-removals"
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body (json/generate-string removal-data))]
                 (:status response) => 403
                 (:body response) => (helpers/json-contains {:reason "Admin credentials are required for this request"})
                 )
               )))

