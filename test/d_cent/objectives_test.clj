(ns d-cent.objectives-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [d-cent.storage :as storage]
            [d-cent.utils :as utils]
            [d-cent.objectives :refer :all]))

(defn requestify [params]
  {:params (assoc params :end-date "2015-01-03")})

(def date-time (utils/string->date-time "2015-01-03"))

(def test-objective {:title "My Objective"
                    :goals "To rock out, All day"
                    :description "I like cake"
                    :end-date date-time})


(def stored-test-objective (assoc test-objective
                            :_id "SOME_GUID"
                            :created-by "username"))

(fact "creates an objective from a request"
      (against-background
        (friend/current-authentication) => {:username "username"})
        (let [objective (request->objective (requestify test-objective))]
          (:created-by objective) => "username"))

(fact "returns nil if extra params are in the request"
      (request->objective (requestify (assoc test-objective :extra-stuff "Blaaaaaaaaah"))) => nil)
