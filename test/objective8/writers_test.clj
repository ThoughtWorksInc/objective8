(ns objective8.writers-test
  (:require [midje.sweet :refer :all]
            [objective8.writers :as writers]
            [objective8.storage.storage :as storage]))

(fact "Postgresql exceptions are not caught"
      (writers/store-invited-writer! {:writer-name "something"}) => (throws org.postgresql.util.PSQLException)
      (provided
        (storage/pg-store! 
          {:entity :invitation :writer-name "something"}) =throws=> (org.postgresql.util.PSQLException.
                                                                    (org.postgresql.util.ServerErrorMessage. "" 0))))
