(ns objective8.unit.objectives-test
  (:require [midje.sweet :refer :all]
            [objective8.objectives :as objectives]
            [objective8.storage.storage :as storage]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)

(fact "Objectives are open when they are not in drafting"
      (objectives/open? {:drafting-started false}) => true
      (objectives/open? {:drafting-started true}) => false)

(fact "Postgresql exceptions are not caught"
      (objectives/store-objective! {:objective "something"}) => (throws org.postgresql.util.PSQLException)
      (provided
        (storage/pg-store! {:entity :objective :objective "something"}) =throws=> (org.postgresql.util.PSQLException.
                                                                                    (org.postgresql.util.ServerErrorMessage. "" 0))))

(fact "Retrieves the first 50 objectives from the database"
      (objectives/retrieve-objectives) => [{:entity :objective :some :content}]
      (provided
        (storage/pg-retrieve {:entity :objective}
                             {:limit 50
                              :sort {:field :_created_at
                                     :ordering :DESC}}) => {:result [{:entity :objective :some :content}]}))
