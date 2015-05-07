(ns objective8.unit.front-end-requests-test
  (:require [midje.sweet :refer :all]
            [objective8.front-end-requests :refer :all]
            [objective8.utils :as utils]))

(def USER_ID 1)

(def test-objective {:title "My Objective"
                     :description "I like cake"})

(def invalid-test-objective {:title ""
                             :description "I like cake"})

(def date (utils/string->date-time "2015-01-01"))
(def date-plus-30-days (utils/string->date-time "2015-01-31"))

(facts "about transforming requests to objective-data"
       (fact "creates an objective from a request"
             (let [objective-data (request->objective-data {:params test-objective} USER_ID date)]
               (:data objective-data) => (assoc test-objective :created-by-id USER_ID :end-date date-plus-30-days)
               (:status objective-data) => ::objective8.front-end-requests/valid
               (:report objective-data) => {}))

       (fact "reports validation errors from a request"
             (let [objective-data (request->objective-data {:params invalid-test-objective} USER_ID date)]
               (:data objective-data) => (assoc invalid-test-objective :created-by-id USER_ID :end-date date-plus-30-days)
               (:status objective-data) => ::objective8.front-end-requests/invalid
               (:report objective-data) => {:title :length})))
