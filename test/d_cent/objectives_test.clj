(ns d-cent.objectives-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [d-cent.storage :as storage]
            [d-cent.utils :as utils]
            [d-cent.objectives :refer :all]))

(defn requestify [params]
  {:params params})

(def test-objective {:title "My Objective"
                    :goals "To rock out, All day"
                    :description "I like cake"
                    :end-date "2015-01-03"})

(def time-stamp (utils/string->time-stamp (:end-date test-objective)))
(def string-time (str time-stamp))

(def stored-test-objective (assoc test-objective
                            :created-by "username"
                            :end-date time-stamp))

(fact "creates correctly formatted objective from a request"
      (against-background
        (friend/current-authentication) => {:username "username"})
        (let [objective (request->objective (requestify test-objective))]
          (:created-by objective) => "username"
          (:end-date objective) => time-stamp))

(fact "returns nil if extra params are in the request"
      (request->objective (requestify (assoc test-objective :extra-stuff "Blaaaaaaaaah"))) => nil)

(fact "stores objective with correct format"
      (store-objective! :the-store (assoc test-objective
                                    :created-by "username"
                                    :end-date string-time))
                                    => (update-in stored-test-objective [:end-date] str)
      (provided (storage/store! :the-store "objectives" stored-test-objective) => stored-test-objective))
