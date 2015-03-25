(ns objective8.unit.actions-test
  (:require [midje.sweet :refer :all]
            [objective8.actions :as actions]
            [objective8.up-down-votes :as up-down-votes]
            [objective8.drafts :as drafts]
            [objective8.objectives :as objectives]
            [objective8.comments :as comments]
            [objective8.storage.storage :as storage]))

(def GLOBAL_ID 6)
(def USER_ID 2)
(def VOTE_ID 5)
(def OBJECTIVE_ID 1)

(facts "about casting up-down votes"
       (fact "stores a vote if user has no active vote on entity"
             (against-background
              (up-down-votes/get-vote GLOBAL_ID USER_ID) => nil)
             (actions/cast-up-down-vote! {:global-id GLOBAL_ID :created-by-id USER_ID :vote-type :up}) => :the-stored-vote
             (provided
              (up-down-votes/store-vote! {:global-id GLOBAL_ID :created-by-id USER_ID :vote-type :up}) => :the-stored-vote))

       (fact "fails if the user is trying to cast another vote on the same entity"
             (actions/cast-up-down-vote! {:global-id GLOBAL_ID :created-by-id USER_ID :vote-type :down}) => nil
             (provided
              (up-down-votes/get-vote GLOBAL_ID USER_ID) => :some-vote)))

(facts "about retrieving drafts"
       (fact "can only retrieve drafts for an objective in drafting"
            (actions/retrieve-drafts OBJECTIVE_ID) => {:status ::actions/objective-drafting-not-started}
            (provided
              (objectives/retrieve-objective OBJECTIVE_ID) => {:drafting-started false}))
       
       (fact "retrieves drafts for an objective that is in drafting"
             (actions/retrieve-drafts OBJECTIVE_ID) => {:status ::actions/success :result :drafts}
             (provided
               (objectives/retrieve-objective OBJECTIVE_ID) => {:drafting-started true}
               (drafts/retrieve-drafts OBJECTIVE_ID) => :drafts))
       
       (fact "can only retrieve latest draft for an objective in drafting"
             (actions/retrieve-latest-draft OBJECTIVE_ID) => {:status ::actions/objective-drafting-not-started}
             (provided
               (objectives/retrieve-objective OBJECTIVE_ID) => {:drafting-started false}))

       (fact "retrieves latest draft for an objective that is in drafting"
             (actions/retrieve-latest-draft OBJECTIVE_ID) => {:status ::actions/success :result :draft}
             (provided
               (objectives/retrieve-objective OBJECTIVE_ID) => {:drafting-started true}
               (drafts/retrieve-latest-draft OBJECTIVE_ID) => :draft)))

(def an-objective-not-in-drafting {:drafting-started false})
(def an-objective-in-drafting {:drafting-started true})
(def comment-data {:comment-on-uri :entity-uri
                   :comment "A comment"
                   :created-by-id USER_ID})

(facts "about commenting"
       (fact "can comment on an objective that is not in drafting"
             (actions/create-comment! comment-data) => {:status ::actions/success :result :the-stored-comment}
             (provided
              (storage/pg-retrieve-entity-by-uri :entity-uri :with-global-id) => an-objective-not-in-drafting
              (comments/store-comment-for! an-objective-not-in-drafting comment-data) => :the-stored-comment))

       (fact "cannot comment on an objective that is in drafting"
             (actions/create-comment! comment-data) => {:status ::actions/objective-drafting-started}
             (provided
              (storage/pg-retrieve-entity-by-uri :entity-uri :with-global-id) => an-objective-in-drafting)))
