(ns objective8.unit.answers-test
  (:require [midje.sweet :refer :all]
            [objective8.back-end.domain.answers :as answers]
            [objective8.back-end.domain.objectives :as objectives]
            [objective8.back-end.storage.storage :as storage]
            [objective8.config :as config]))

(fact "Postgresql exceptions are not caught"
      (answers/store-answer! {:answer "something"}) => (throws org.postgresql.util.PSQLException)
      (provided
        (storage/pg-store! {:entity :answer :answer "something"}) =throws=> (org.postgresql.util.PSQLException.
                                                                              (org.postgresql.util.ServerErrorMessage. "" 0)))) 
