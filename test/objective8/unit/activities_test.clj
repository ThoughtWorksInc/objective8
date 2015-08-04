(ns objective8.unit.activities-test
  (:require [midje.sweet :refer :all]
            [objective8.back-end.domain.activities :as activities]))

(fact "can convert an objective into an activity"
     (activities/objective->activity {:username "UserName"
                                      :title "Title"
                                      :description "Description"
                                      :uri "/objectives/21"
                                      :_created_at "2015-08-03T16:09:35.143Z"})
    => {"@context" "http://www.w3.org/ns/activitystreams"
        "@type" "Create"
        "published" "2015-08-03T16:09:35.143Z"
        "actor" {"@type" "Person"
                 "displayName" "UserName"}
        "object" {"@type" "Objective"
                  "displayName" "Title"
                  "content" "Description"
                  "url" "http://localhost:8080/objectives/21"}})
