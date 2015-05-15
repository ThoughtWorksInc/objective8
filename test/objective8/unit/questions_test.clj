(ns objective8.unit.questions-test
  (:require [midje.sweet :refer :all]
            [objective8.back-end.questions :as questions]
            [objective8.back-end.objectives :as objectives]
            [objective8.back-end.storage.storage :as storage]
            [objective8.config :as config]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)

(def question {:objective-id OBJECTIVE_ID})

(binding [config/two-phase? true]
  (fact "A question can be created when the associated objective is not in drafting"
        (questions/create-question question) => :stored-question
        (provided
          (objectives/get-objective OBJECTIVE_ID) => {:status "open"}
          (questions/store-question! question) => :stored-question)) 

  (fact "Attempting to create a question against an objective that is in drafting returns nil"
        (questions/create-question question) => nil
        (provided
          (objectives/get-objective OBJECTIVE_ID) => {:status "drafting"}))) 

(fact "Postgresql exceptions are not caught"
      (questions/store-question! {:question "something"}) => (throws org.postgresql.util.PSQLException)
      (provided
        (storage/pg-store! {:entity :question :question "something"}) =throws=> (org.postgresql.util.PSQLException.
                                                                                  (org.postgresql.util.ServerErrorMessage. "" 0))))
