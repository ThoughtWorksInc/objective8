(ns objective8.unit.invitations-test
  (:require [midje.sweet :refer :all]
            [objective8.back-end.domain.invitations :as invitations]
            [objective8.back-end.storage.storage :as storage]
            [objective8.back-end.domain.objectives :as objectives]
            [objective8.config :as config]
            [objective8.utils :as utils]))

(def OBJECTIVE_ID 1)
(def invitation {:objective-id OBJECTIVE_ID})


(facts "about accepting an invitation"
       (fact "returns the accepted invitation when the associated objective is not in drafting"
             (invitations/accept-invitation! invitation) => :accepted-invitation
             (provided
               (storage/pg-update-invitation-status! invitation "accepted") => :accepted-invitation)))


(facts "about declining an invitation"
       (fact "returns the declined invitation when the associated objective is not in drafting"
             (invitations/decline-invitation! invitation) => :declined-invitation
             (provided
               (storage/pg-update-invitation-status! invitation "declined") => :declined-invitation)))

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

