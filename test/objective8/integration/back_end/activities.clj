(ns objective8.integration.back-end.activities
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.middleware :as m]))

(def app (helpers/api-context))

(background
 (m/valid-credentials? anything anything anything) => true)

(against-background
  [(before :contents (helpers/db-connection))]

  (facts "GET /api/v1/activities returns the activities"
         (fact "activities are returned as a json array"
               (let [stored-activities (doall (repeatedly 5 sh/store-an-activity))
                     {response :response} (p/request app (utils/api-path-for :api/get-activities))]
                 (json/parse-string (:body response)) => (reverse stored-activities)))

         (fact "returns an empty body if there are no activities"
               (do
                 (helpers/truncate-tables)
                 (:body (p/request app (utils/api-path-for :api/get-activities))))
               => empty?))

  (facts "GET /api/v1/activities supports offset and limit"
         (helpers/truncate-tables)
         (let [a1 (sh/store-an-activity)
               a2 (sh/store-an-activity)
               a3 (sh/store-an-activity)
               a4 (sh/store-an-activity)
               {response :response} (p/request app (str (utils/api-path-for :api/get-activities) "?offset=1&limit=2"))]
           (json/parse-string (:body response)) => [a3 a2])))
