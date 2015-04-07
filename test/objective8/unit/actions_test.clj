(ns objective8.unit.actions-test
  (:require [midje.sweet :refer :all]
            [objective8.actions :as actions]
            [objective8.up-down-votes :as up-down-votes]
            [objective8.drafts :as drafts]
            [objective8.objectives :as objectives]
            [objective8.comments :as comments]
            [objective8.stars :as stars]
            [objective8.storage.storage :as storage]))

(def GLOBAL_ID 6)
(def USER_ID 2)
(def VOTE_ID 5)
(def OBJECTIVE_ID 1)
(def an-answer {:global-id GLOBAL_ID})
(def vote-data {:vote-on-uri :entity-uri
                :created-by-id USER_ID
                :vote-type :up})

(facts "about casting up-down votes"
       (against-background
        (actions/allowed-to-vote? anything anything) => true)
       (fact "stores a vote if user has no active vote on entity"
             (against-background
              (up-down-votes/get-vote GLOBAL_ID USER_ID) => nil
              (storage/pg-retrieve-entity-by-uri :entity-uri :with-global-id) => an-answer)
             (actions/cast-up-down-vote! vote-data) => {:status ::actions/success 
                                                        :result :the-stored-vote}
             (provided
              (up-down-votes/store-vote! an-answer vote-data) => :the-stored-vote))

       (fact "fails if voting is not allowed on this entity"
             (against-background
               (storage/pg-retrieve-entity-by-uri :entity-uri :with-global-id) => an-answer)
             (actions/cast-up-down-vote! vote-data) => {:status ::actions/forbidden}
             (provided
              (actions/allowed-to-vote? an-answer vote-data) => false))

       (fact "reports an error when the entity to vote on cannot be found"
             (against-background
              (storage/pg-retrieve-entity-by-uri :entity-uri :with-global-id) => nil)
             (actions/cast-up-down-vote! vote-data) => {:status ::actions/entity-not-found}))

(def objective-in-drafting {:entity :objective
                            :status "drafting"})

(def objective-not-in-drafting {:entity :objective
                                :status "open"})

(facts "about allowing or disallowing voting"
       (against-background
        (up-down-votes/get-vote anything anything) => nil
        (objectives/retrieve-objective anything) => objective-not-in-drafting
        (objectives/retrieve-objective :objective-in-drafting) => objective-in-drafting
        (objectives/retrieve-objective :objective-not-in-drafting) => objective-not-in-drafting
        (storage/pg-retrieve-entity-by-global-id :objective-in-drafting-global-id) => objective-in-drafting)
       
       (fact "the same user cannot vote twice on the same entity"
             (actions/allowed-to-vote? objective-not-in-drafting :vote-data) => falsey
             (provided
              (up-down-votes/get-vote anything anything) => :a-vote))
       
       (fact "a comment attached to an objective can not be voted on when the objective is in drafting"
             (actions/allowed-to-vote? {:entity :comment
                                        :objective-id :objective-in-drafting
                                        :comment-on-id :objective-in-drafting-global-id} :vote-data) => falsey)

       (fact "a comment attached to a draft can not be voted on when the associated objective is not in drafting"
             (against-background
              (storage/pg-retrieve-entity-by-global-id :draft-global-id) => {:entity :draft
                                                                             :objective-id :objective-not-in-drafting})
             (actions/allowed-to-vote? {:entity :comment
                                        :comment-on-id :draft-global-id} :vote-data) => falsey)
       
       (fact "an answer can not be voted on when the associated objective is in drafting"
             (actions/allowed-to-vote? {:entity :answer
                                        :objective-id :objective-in-drafting} :vote-data) => falsey))

(facts "about retrieving drafts"
       (fact "can only retrieve drafts for an objective in drafting"
            (actions/retrieve-drafts OBJECTIVE_ID) => {:status ::actions/objective-drafting-not-started}
            (provided
              (objectives/retrieve-objective OBJECTIVE_ID) => {:status "open"}))
       
       (fact "retrieves drafts for an objective that is in drafting"
             (actions/retrieve-drafts OBJECTIVE_ID) => {:status ::actions/success :result :drafts}
             (provided
               (objectives/retrieve-objective OBJECTIVE_ID) => {:status "drafting"}
               (drafts/retrieve-drafts OBJECTIVE_ID) => :drafts))
       
       (fact "can only retrieve latest draft for an objective in drafting"
             (actions/retrieve-latest-draft OBJECTIVE_ID) => {:status ::actions/objective-drafting-not-started}
             (provided
               (objectives/retrieve-objective OBJECTIVE_ID) => {:status "open"}))

       (fact "retrieves latest draft for an objective that is in drafting"
             (actions/retrieve-latest-draft OBJECTIVE_ID) => {:status ::actions/success :result :draft}
             (provided
               (objectives/retrieve-objective OBJECTIVE_ID) => {:status "drafting"} 
               (drafts/retrieve-latest-draft OBJECTIVE_ID) => :draft)))

(def a-draft {:entity :draft})
(def comment-data {:comment-on-uri :entity-uri
                   :comment "A comment"
                   :created-by-id USER_ID})

(facts "about creating comments"
       (fact "can comment on an objective that is not in drafting"
             (actions/create-comment! comment-data) => {:status ::actions/success :result :the-stored-comment}
             (provided
              (storage/pg-retrieve-entity-by-uri :entity-uri :with-global-id) => objective-not-in-drafting
              (comments/store-comment-for! objective-not-in-drafting comment-data) => :the-stored-comment))

       (fact "cannot comment on an objective that is in drafting"
             (actions/create-comment! comment-data) => {:status ::actions/objective-drafting-started}
             (provided
              (storage/pg-retrieve-entity-by-uri :entity-uri :with-global-id) => objective-in-drafting))

       (fact "can comment on a draft"
             (actions/create-comment! comment-data) => {:status ::actions/success :result :the-stored-comment}
             (provided
              (storage/pg-retrieve-entity-by-uri :entity-uri :with-global-id) => a-draft
              (comments/store-comment-for! a-draft comment-data) => :the-stored-comment))

       (fact "reports an error when the entity to comment on cannot be found"
             (actions/create-comment! comment-data) => {:status ::actions/entity-not-found}
             (provided
              (storage/pg-retrieve-entity-by-uri anything anything) => nil)))

(facts "about getting comments"
       (fact "gets comments for an entity identified by URI"
             (actions/get-comments :uri) => {:status ::actions/success
                                             :result :comments}
             (provided
              (comments/get-comments :uri) => :comments))

       (fact "reports an error when the entity cannot be found"
             (actions/get-comments :uri) => {:status ::actions/entity-not-found}
             (provided
              (comments/get-comments :uri) => nil)))

(def star-data {:objective-id OBJECTIVE_ID
                :created-by-id USER_ID})

(facts "about toggling stars"
       (fact "star is created for an objective"
             (actions/toggle-star! star-data) => {:status ::actions/success
                                                  :result :the-stored-star}
             (provided
               (stars/retrieve-star OBJECTIVE_ID USER_ID) => nil
               (stars/store-star! star-data) => :the-stored-star)))
