(ns d-cent.proposals-test
  (:require [midje.sweet :refer :all]
            [d-cent.proposals :refer :all]))

(defn requestify [params]
  {:params params})

(def test-proposal {:title "My Proposal"
                    :objectives ["To rock out" "All day"]
                    :description "I like cake"})

(fact "gets a proposal from a request"
      (request->proposal (requestify test-proposal)) => test-proposal)

(fact "ignores extra content"
      (request->proposal (requestify (assoc test-proposal :extra-stuff "Blaaaaaaaaah"))) => test-proposal)

(fact "adds author to proposal")
