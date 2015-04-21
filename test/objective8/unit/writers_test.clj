(ns objective8.unit.writers-test
  (:require [midje.sweet :refer :all]
            [objective8.writers :as writers]
            [objective8.invitations :as i]
            [objective8.utils :as utils]
            [objective8.storage.storage :as storage]))

(def OBJECTIVE_ID 123)
(def INVITATION_ID 4)
(def USER_ID 3)
(def INVITED_BY_ID 2)
(def UUID "some uuid")

(defn database-exception []
  (org.postgresql.util.PSQLException.
   (org.postgresql.util.ServerErrorMessage. "" 0)))

(fact "creating a writer accepts the invitation and returns the created writer"
      (against-background
        (utils/select-all-or-nothing anything anything) => {}
        (utils/ressoc anything anything anything) => {})

      (writers/create-writer {:invitation-uuid UUID}) => :new-writer
      (provided
        (i/get-active-invitation UUID) => :active-invitation
        (i/accept-invitation! :active-invitation) => :accepted-invitation
        (storage/pg-store! anything) => :new-writer))

(fact "creating a writer fails when an active invitation is not provided"
      (writers/create-writer {:invitation-uuid UUID}) => nil
      (provided
       (i/get-active-invitation UUID) => nil))

(facts "throws exception when database errors occur"
       (against-background
         (i/get-active-invitation anything) => {}
         (i/accept-invitation! anything) => {}
         (utils/select-all-or-nothing anything anything) => {} 
         (utils/ressoc anything anything anything) => {}) 

       (fact "while checking the invitation"
             (writers/create-writer {}) => (throws Exception "Failed to create writer")
             (provided
              (i/get-active-invitation anything) =throws=> (database-exception)))

       (fact "while accepting the invitation"
             (writers/create-writer {}) => (throws Exception "Failed to create writer")
             (provided
              (i/accept-invitation! anything) =throws=> (database-exception)))

       (fact "while creating the writer"
             (writers/create-writer {}) => (throws Exception "Failed to create writer")
             (provided
              (storage/pg-store! anything) =throws=> (database-exception))))

(fact "By default, only the first 50 writers are retrieved"
      (writers/retrieve-writers OBJECTIVE_ID) => anything
      (provided
        (storage/pg-retrieve {:entity :writer
                              :objective-id OBJECTIVE_ID}
                             {:limit 50}) => []))
