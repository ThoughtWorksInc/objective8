(ns objective8.unit.up-down-votes-test
  (:require [midje.sweet :refer :all]
            [objective8.up-down-votes :refer :all]
            [objective8.storage.storage :as storage]))

(def VOTE_ID 1)
(def UEID 2)
(def USER_ID 3)

(def previous-vote {:_id VOTE_ID :ueid UEID :user-id USER_ID :vote-type :up})
(def new-vote-data {:ueid UEID :user-id USER_ID :vote-type :down})
(def new-vote (assoc new-vote-data :entity :up-down-vote :active true))

(fact "update-vote! nullifies previous vote before storing vote"
      (update-vote! previous-vote new-vote-data) => :new-stored-vote
      (provided
        (nullify-vote! previous-vote) => :nullified-previous-vote
        (store-vote! new-vote-data) => :new-stored-vote))

(fact "votes are active when stored"
      (store-vote! new-vote-data) => :new-stored-vote
      (provided
        (storage/pg-store! new-vote) => :new-stored-vote))
