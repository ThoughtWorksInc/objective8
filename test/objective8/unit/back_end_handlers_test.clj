(ns objective8.unit.back-end-handlers-test
  (:require [midje.sweet :refer :all]
            [objective8.utils :as utils]
            [objective8.back-end.handlers :as bh]))

(def activities (repeat {"@context" "http://www.w3.org/ns/activitystreams"}))

(facts "about wrapping activity stream as a JSON-LD ordered collection"
       (fact "@context and @type are set correctly"
             (bh/wrap-as-ordered-collection [] 0 0)
             => (contains {"@context" "http://www.w3.org/ns/activitystreams"
                           "@type" "OrderedCollection"}))

       (fact "items do not include the json-ld context"
             (get (bh/wrap-as-ordered-collection (take 5 activities) 10 0) "items")
             =not=> (has some (contains {"@context" anything})))

       (tabular
        (fact "sets itemsPerPage and startIndex correctly based on limit and offset"
              (bh/wrap-as-ordered-collection [] ?limit ?offset) => (contains ?expected))
        ?limit    ?offset     ?expected
        10        0           {"itemsPerPage" 10 "startIndex" 0}
        nil       10          {"startIndex" 10}
        10        nil         {"itemsPerPage" 10 "startIndex" 0})

       (facts "about the prev link"
              (tabular
               (fact "excluded when no previous page exists"
                     (bh/wrap-as-ordered-collection [] ?limit ?offset) =not=> (contains {"prev" anything}))
               ?limit    ?offset
               10        0
               10        nil
               nil       0
               nil       10)

              (fact "included when previous pages exist"
                    (bh/wrap-as-ordered-collection [] 10 1) => (contains {"prev" anything})))

       (facts "about the next link"
              (tabular
               (fact "excluded when no further pages exist"
                     (bh/wrap-as-ordered-collection ?activities ?limit ?offset)
                     =not=> (contains {"next" anything}))
               ?activities          ?limit     ?offset
               (take 3 activities)  5          0
               (take 5 activities)  nil        nil
               (take 5 activities)  nil        1)

              (tabular
               (fact "included when further pages exist"
                     (bh/wrap-as-ordered-collection ?activities ?limit ?offset)
                     => (contains {"next" anything}))
               ?activities          ?limit     ?offset
               (take 5 activities)  5          0
               (take 5 activities)  5          nil)))

(fact "activity query format string is generated correctly, depending on whether a limit was provided"
      (bh/activity-query-format-string nil) => (str (utils/api-path-for :api/get-activities)
                                                    "?offset=%s"
                                                    "&as_ordered_collection=true")
      (bh/activity-query-format-string 10) => (str (utils/api-path-for :api/get-activities)
                                                    "?offset=%s"
                                                    "&limit=10"
                                                    "&as_ordered_collection=true"))
