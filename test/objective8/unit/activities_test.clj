(ns objective8.unit.activities-test
  (:require [midje.sweet :refer :all]
            [objective8.back-end.domain.activities :as activities]
            [objective8.back-end.domain.objectives :as objectives]))

(def mock-objective {:username    "UserName"
                     :title       "Title"
                     :description "Description"
                     :uri         "/objectives/21"
                     :_created_at "2015-08-03T16:09:35.143Z"})

(fact "can convert an objective into an activity"
      (activities/objective->activity mock-objective)
      => {"@context"  "http://www.w3.org/ns/activitystreams"
          "type"      "Create"
          "published" "2015-08-03T16:09:35.143Z"
          "actor"     {"type" "Person"
                       "name" "UserName"}
          "object"    {"type"    "Objective"
                       "name"    "Title"
                       "content" "Description"
                       "url"     "http://localhost:8080/objectives/21"}})

(future-fact "can convert a question into an activity"
             (activities/question->activity {:username     "UserName"
                                             :title        "Title"
                                             :uri          "/objectives/21/questions/4"
                                             :_created_at  "2015-08-03T16:09:35.143Z"
                                             :question     "Question"
                                             :objective_id 21})
             => {"@context"  "http://www.w3.org/ns/activitystreams"
                 "type"      "Question"
                 "published" "2015-08-03T16:09:35.143Z"
                 "actor"     {"type" "Person"
                              "name" "Username"}
                 "object"    {"type" "Objective Question"
                              "name" "Question"
                              "url"  "http://localhost:8080/objectives/21/questions/4"}
                 "target"    {"type" "Objective"
                              "name" "Title"
                              "url"  "http://localhost:8080/objectives/21"}}
             (provided
               (objectives/get-objective 21) => mock-objective))
