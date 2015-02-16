(ns objective8.questions-test
  (:require [midje.sweet :refer :all]
            [objective8.questions :as questions]
            [objective8.storage.storage :as storage]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)

(fact "Postgresql exceptions are not caught"
      (questions/store-question! {:question "something"}) => (throws org.postgresql.util.PSQLException)
      (provided
        (storage/pg-store! {:entity :question :question "something"}) =throws=> (org.postgresql.util.PSQLException.
                                                                                  (org.postgresql.util.ServerErrorMessage. "" 0))))
