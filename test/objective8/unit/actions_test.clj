(ns objective8.unit.actions-test
  (:require [midje.sweet :refer :all]
            [objective8.actions :as actions]
            [objective8.up-down-votes :as up-down-votes]))

(def UEID 6)
(def USER_ID 2)
(def VOTE_ID 5)

(facts "about casting up-down votes"
       (fact "stores a vote if user has no active vote on entity"
             (against-background
              (up-down-votes/get-active-vote UEID USER_ID) => nil)
             (actions/cast-up-down-vote! {:ueid UEID :user-id USER_ID :vote-type :up}) => :the-stored-vote
             (provided
              (up-down-votes/store-vote! {:ueid UEID :user-id USER_ID :vote-type :up}) => :the-stored-vote))

       (fact "checks whether the user is trying to recast the same vote"
             (actions/cast-up-down-vote! {:ueid UEID :user-id USER_ID :vote-type :up}) => nil
             (provided
              (up-down-votes/get-active-vote UEID USER_ID) => {:_id VOTE_ID :ueid UEID :user-id USER_ID :vote-type :up}))

       (fact "updates vote if user is changing vote"
             (against-background
              (up-down-votes/get-active-vote UEID USER_ID) => {:vote-type :down})
             (actions/cast-up-down-vote! {:ueid UEID :user-id USER_ID :vote-type :up}) => :the-stored-vote
             (provided
              (up-down-votes/update-vote! {:vote-type :down} {:ueid UEID :user-id USER_ID :vote-type :up}) => :the-stored-vote)))
