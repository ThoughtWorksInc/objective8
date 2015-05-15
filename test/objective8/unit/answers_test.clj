(ns objective8.unit.answers-test
  (:require [midje.sweet :refer :all]
            [objective8.back-end.answers :as answers]
            [objective8.back-end.objectives :as objectives]
            [objective8.back-end.storage.storage :as storage]
            [objective8.config :as config]))

(def USER_ID 1)
(def QUESTION_ID 234)
(def OBJECTIVE_ID 1)

(def answer {:objective-id OBJECTIVE_ID})

(binding [config/two-phase? true]
  (fact "An answer can be created when the associated objective is not in drafting"
        (answers/create-answer! answer) => :stored-answer
        (provided
          (objectives/get-objective OBJECTIVE_ID) => {:status "open"}
          (answers/store-answer! answer) => :stored-answer)) 

  (fact "Returns nil when the associated objective is not in drafting"
        (answers/create-answer! answer) => nil
        (provided
          (objectives/get-objective OBJECTIVE_ID) => {:status "drafting"}))) 

(fact "Postgresql exceptions are not caught"
      (answers/store-answer! {:answer "something"}) => (throws org.postgresql.util.PSQLException)
      (provided
        (storage/pg-store! {:entity :answer :answer "something"}) =throws=> (org.postgresql.util.PSQLException.
                                                                              (org.postgresql.util.ServerErrorMessage. "" 0)))) 
