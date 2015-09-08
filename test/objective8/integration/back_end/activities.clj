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
               (helpers/truncate-tables)
               (let [stored-activities (doall (repeatedly 5 sh/store-an-activity))
                     {response :response} (p/request app (utils/api-path-for :api/get-activities))]
                 (json/parse-string (:body response)) => (reverse stored-activities)))

         (fact "returns an empty body if there are no activities"
               (do
                 (helpers/truncate-tables)
                 (:body (p/request app (utils/api-path-for :api/get-activities))))
               => empty?)

         ;; TODO 20150908 DM - Firefox doesn't seem to support using
         ;; the +json suffix to indicate interpreting non-standard
         ;; mime-types as json (see http://tools.ietf.org/html/rfc6839#page-4).
         ;; Thus, this breaks the functional tests.
         (future-fact "has the correct content type"
                      (get-in (p/request app (utils/api-path-for :api/get-activities))
                              [:response :headers "Content-Type"])
                      => "application/activity+json"))

  (fact "GET /api/v1/activities supports offset and limit"
        (helpers/truncate-tables)
        (let [a1 (sh/store-an-activity)
              a2 (sh/store-an-activity)
              a3 (sh/store-an-activity)
              a4 (sh/store-an-activity)
              {response :response} (p/request app (str (utils/api-path-for :api/get-activities)
                                                       "?offset=1"
                                                       "&limit=2"))]
          (json/parse-string (:body response)) => [a3 a2]))

  (fact "GET /api/v1/activities optionally responds with an activity streams 2.0 OrderedCollection Json-LD document"
        (helpers/truncate-tables)
        (let [[_ _ _ a4 a5 _] (doall (repeatedly 6 sh/store-an-activity))
              {response :response} (p/request app (str (utils/api-path-for :api/get-activities)
                                                       "?offset=1"
                                                       "&limit=2"
                                                       "&as_ordered_collection=true"))]
          (json/parse-string (:body response))
          => (just {"@context" "http://www.w3.org/ns/activitystreams"
                    "@type" "OrderedCollection"
                    "itemsPerPage" 2
                    "startIndex" 1
                    "first" (str (utils/api-path-for :api/get-activities) "?offset=0&limit=2&as_ordered_collection=true")
                    "prev" (str (utils/api-path-for :api/get-activities) "?offset=0&limit=2&as_ordered_collection=true")
                    "next" (str (utils/api-path-for :api/get-activities) "?offset=3&limit=2&as_ordered_collection=true")
                    "items" [(dissoc a5 "@context")
                             (dissoc a4 "@context")]}))))
