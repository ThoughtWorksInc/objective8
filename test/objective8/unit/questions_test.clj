(ns objective8.unit.questions-test
  (:require [midje.sweet :refer :all]
            [objective8.back-end.domain.questions :as questions]
            [objective8.back-end.domain.objectives :as objectives]
            [objective8.back-end.storage.storage :as storage]
            [objective8.config :as config]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)

(fact "Postgresql exceptions are not caught"
      (questions/store-question! {:question "something"}) => (throws org.postgresql.util.PSQLException)
      (provided
        (storage/pg-store! {:entity :question :question "something"}) =throws=> (org.postgresql.util.PSQLException.
                                                                                  (org.postgresql.util.ServerErrorMessage. "" 0))))
