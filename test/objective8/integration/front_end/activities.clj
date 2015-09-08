(ns objective8.integration.front-end.activities
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [objective8.front-end.api.http :as http-api]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as ih]))

(fact "activity stream can be retrieved"
      (against-background
        (http-api/get-activities "2" "3") => "[json-blob]")
      (let [{response :response} (-> (ih/front-end-context)
                                     (p/request (str (utils/path-for :fe/activities) "?limit=2&offset=3")))]
        (:status response) => 200
        (get-in response [:headers "Content-Type"]) => "application/json"
        (:body response) => "[json-blob]"))

(fact "activity stream can be retrieved, wrapped as an activity streams 2.0 OrderedCollection structure"
      (against-background
       (http-api/get-activities "2" "3" true) => ""))
