(ns d-cent.objectives-test
  (:require [midje.sweet :refer :all]
            [d-cent.objectives :refer :all]))

(defn requestify [params]
  {:params params})

(def test-objective {:title "My Objective"
                    :actions ["To rock out" "All day"]
                    :description "I like cake"
                    :end-date "2015-01-31"})

(fact "gets an objective from a request"
      (request->objective (requestify test-objective)) => test-objective)

(fact "ignores extra content"
      (request->objective (requestify (assoc test-objective :extra-stuff "Blaaaaaaaaah"))) => test-objective)

(fact "adds author to objective")
