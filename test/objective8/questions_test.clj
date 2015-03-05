(ns objective8.questions-test
  (:require [midje.sweet :refer :all]
            [objective8.questions :as questions]
            [objective8.objectives :as objectives]
            [objective8.storage.storage :as storage]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)

(def question {:objective-id OBJECTIVE_ID})

(fact "A question can be created when the associated objective is not in drafting"
      (questions/create-question question) => :stored-question
      (provided
        (objectives/retrieve-objective OBJECTIVE_ID) => {:drafting-started false}
        (questions/store-question! question) => :stored-question))

(fact "Attempting to create a question against an objective that is in drafting returns nil"
      (questions/create-question question) => nil
      (provided
        (objectives/retrieve-objective OBJECTIVE_ID) => {:drafting-started true}))

(fact "Postgresql exceptions are not caught"
      (questions/store-question! {:question "something"}) => (throws org.postgresql.util.PSQLException)
      (provided
        (storage/pg-store! {:entity :question :question "something"}) =throws=> (org.postgresql.util.PSQLException.
                                                                                  (org.postgresql.util.ServerErrorMessage. "" 0))))
