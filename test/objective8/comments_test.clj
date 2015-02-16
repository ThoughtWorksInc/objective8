(ns objective8.comments-test
  (:require [midje.sweet :refer :all]
            [objective8.comments :as comments]
            [objective8.storage.storage :as storage]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)


(fact "By default, only the first 50 comments are retrieved"
      (comments/retrieve-comments OBJECTIVE_ID) => anything
      (provided
       (storage/pg-retrieve {:entity :comment
                             :objective-id OBJECTIVE_ID}
                            {:limit 50}) => []))

(fact "Postgresql exceptions are not caught"
      (comments/store-comment! {:comment "something"}) => (throws org.postgresql.util.PSQLException)
      (provided
        (storage/pg-store! {:entity :comment :comment "something"}) =throws=> (org.postgresql.util.PSQLException.
                                                                               (org.postgresql.util.ServerErrorMessage. "" 0))))
