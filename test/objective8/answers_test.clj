(ns objective8.answers-test
  (:require [midje.sweet :refer :all]
            [objective8.answers :as answers]
            [objective8.storage.storage :as storage]))

(def USER_ID 1)
(def QUESTION_ID 234)

(fact "Postgresql exceptions are not caught"
      (answers/store-answer! {:answer "something"}) => (throws org.postgresql.util.PSQLException)
      (provided
        (storage/pg-store! {:entity :answer :answer "something"}) =throws=> (org.postgresql.util.PSQLException.
                                                                              (org.postgresql.util.ServerErrorMessage. "" 0))))

(fact "By default, only the first 50 answers are retrieved"
      (answers/retrieve-answers QUESTION_ID) => anything 
      (provided
        (storage/pg-retrieve {:entity :answer
                              :question-id QUESTION_ID}
                             {:limit 50}) => []))
