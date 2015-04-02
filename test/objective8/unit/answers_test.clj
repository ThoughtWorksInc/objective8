(ns objective8.unit.answers-test
  (:require [midje.sweet :refer :all]
            [objective8.answers :as answers]
            [objective8.objectives :as objectives]
            [objective8.storage.storage :as storage]))

(def USER_ID 1)
(def QUESTION_ID 234)
(def OBJECTIVE_ID 1)

(def answer {:objective-id OBJECTIVE_ID})

(fact "An answer can be created when the associated objective is not in drafting"
      (answers/create-answer! answer) => :stored-answer
      (provided
        (objectives/retrieve-objective OBJECTIVE_ID) => {:status "open"}
        (answers/store-answer! answer) => :stored-answer))

(fact "Returns nil when the associated objective is not in drafting"
      (answers/create-answer! answer) => nil
      (provided
        (objectives/retrieve-objective OBJECTIVE_ID) => {:status "drafting"}))

(fact "Postgresql exceptions are not caught"
      (answers/store-answer! {:answer "something"}) => (throws org.postgresql.util.PSQLException)
      (provided
        (storage/pg-store! {:entity :answer :answer "something"}) =throws=> (org.postgresql.util.PSQLException.
                                                                              (org.postgresql.util.ServerErrorMessage. "" 0))))
