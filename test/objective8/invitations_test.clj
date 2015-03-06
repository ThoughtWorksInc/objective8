(ns objective8.invitations-test
  (:require [midje.sweet :refer :all]
            [objective8.invitations :as invitations]
            [objective8.storage.storage :as storage]
            [objective8.objectives :as objectives]
            [objective8.utils :as utils]))

(def OBJECTIVE_ID 1)
(def invitation {:objective-id OBJECTIVE_ID})

(fact "An invitation can be created when the associated objective is not in drafting"
      (invitations/create-invitation! invitation) => :stored-invitation
      (provided
        (objectives/retrieve-objective OBJECTIVE_ID) => {:drafting-started false}
        (invitations/store-invitation! invitation) => :stored-invitation))

(fact "Returns nil when the associated objective is not in drafting"
      (invitations/create-invitation! invitation) => nil
      (provided
        (objectives/retrieve-objective OBJECTIVE_ID) => {:drafting-started true}))

(fact "Postgresql exceptions are not caught"
      (against-background
        (utils/generate-random-uuid) => "random-uuid")
      (invitations/store-invitation! {:writer-name "something"}) => (throws org.postgresql.util.PSQLException)
      (provided
        (storage/pg-store!
          {:entity :invitation
           :writer-name "something"
           :uuid "random-uuid"
           :status "active"}) =throws=> (org.postgresql.util.PSQLException.
                                          (org.postgresql.util.ServerErrorMessage. "" 0))))

(fact "creates an invitation with a random uuid"
      (invitations/store-invitation! {:writer-name "something"}) => :stored-invitation
      (provided
        (utils/generate-random-uuid) => "random-uuid"
        (storage/pg-store! {:entity :invitation :writer-name "something" :uuid "random-uuid" :status "active"}) => :stored-invitation))

