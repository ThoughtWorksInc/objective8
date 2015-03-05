(ns objective8.comments-test
  (:require [midje.sweet :refer :all]
            [objective8.comments :as comments]
            [objective8.objectives :as objectives]
            [objective8.storage.storage :as storage]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)

(def comment {:objective-id OBJECTIVE_ID})

(fact "A comment can be created when the associated objective is not in drafting"
      (comments/create-comment comment) => :stored-comment
      (provided
       (objectives/retrieve-objective OBJECTIVE_ID) => {:drafting-started false}
       (comments/store-comment! comment) => :stored-comment))

(fact "Attempting to create a comment against an objective that is in drafting returns nil"
      (comments/create-comment comment) => nil
      (provided
        (objectives/retrieve-objective OBJECTIVE_ID) => {:drafting-started true}))


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
