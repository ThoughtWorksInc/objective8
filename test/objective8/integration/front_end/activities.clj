(ns objective8.integration.front-end.activities
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.front-end.api.http :as http-api]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as ih]))

(fact "admin removals can be retrieved"
      (against-background
        (http-api/get-activities) => "[json-blob]")
      (let [{response :response} (-> (ih/front-end-context)
                                     (p/request (utils/path-for :fe/activities)))]
        (:status response) => 200
        (get-in response [:headers "Content-Type"]) => "application/json"
        (:body response) => "[json-blob]"))
