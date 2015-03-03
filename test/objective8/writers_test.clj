(ns objective8.writers-test
  (:require [midje.sweet :refer :all]
            [objective8.writers :as writers]
            [objective8.utils :as utils]
            [objective8.storage.storage :as storage]))

(def OBJECTIVE_ID 123)
(def INVITATION_ID 4)
(def USER_ID 3)
(def INVITED_BY_ID 2)
(def UUID "some uuid")

(fact "Postgresql exceptions are not caught"
      (against-background
        (utils/generate-random-uuid) => "random-uuid")
      (writers/store-invitation! {:writer-name "something"}) => (throws org.postgresql.util.PSQLException)
      (provided
        (storage/pg-store!
          {:entity :invitation
           :writer-name "something"
           :uuid "random-uuid"
           :status "active"}) =throws=> (org.postgresql.util.PSQLException.
                                          (org.postgresql.util.ServerErrorMessage. "" 0))))

(fact "creates an invitation with a random uuid"
      (writers/store-invitation! {:writer-name "something"}) => :stored-invitation
      (provided
        (utils/generate-random-uuid) => "random-uuid"
        (storage/pg-store! {:entity :invitation :writer-name "something" :uuid "random-uuid" :status "active"}) => :stored-invitation))

(fact "creating a candidate accepts the invitation and returns the created candidate"
      (writers/create-candidate {:invitation-uuid UUID}) => :new-candidate
      (provided
       (storage/pg-update-invitation-status! UUID "accepted") => :accepted-invitation
       (storage/pg-store! anything) => :new-candidate))

(fact "By default, only the first 50 candidates are retrieved"
      (writers/retrieve-candidates OBJECTIVE_ID) => anything
      (provided
        (storage/pg-retrieve {:entity :candidate
                              :objective-id OBJECTIVE_ID}
                             {:limit 50}) => []))
