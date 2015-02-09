(ns objective8.objectives-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [objective8.storage.storage :as storage]
            [objective8.utils :as utils]
            [objective8.objectives :refer :all]))

(defn requestify [params]
  {:params (assoc params :end-date "2015-01-03")})

(def date-time (utils/string->date-time "2015-01-03"))

(def test-objective {:title "My Objective"
                    :goals "To rock out, All day"
                    :description "I like cake"
                    :end-date date-time})


(def stored-test-objective (assoc test-objective
                            :_id 123
                            :created-by "username"))

(fact "creates an objective from a request"
      (against-background
        (friend/current-authentication) => {:username 1})
        (let [objective (request->objective (requestify test-objective))]
          (:created-by-id objective) => 1))
