(ns objective8.unit.actions-test
  (:require [midje.sweet :refer :all]
            [objective8.actions :as actions]
            [objective8.up-down-votes :as up-down-votes]
            [objective8.drafts :as drafts]
            [objective8.objectives :as objectives]
            [objective8.comments :as comments]
            [objective8.users :as users]
            [objective8.writers :as writers]
            [objective8.invitations :as invitations]
            [objective8.stars :as stars]
            [objective8.questions :as questions]
            [objective8.marks :as marks]
            [objective8.writer-notes :as writer-notes]
            [objective8.storage.storage :as storage]))

(def GLOBAL_ID 6)
(def USER_ID 2)
(def VOTE_ID 5)
(def OBJECTIVE_ID 1)
(def QUESTION_ID 2)
(def ANSWER_ID 3)
(def ANSWER_URI (str "/objective/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers/" ANSWER_ID))
(def INVITATION_ID 7)
(def UU_ID "875678950430596859403-uuid")
(def DRAFT_ID 8)
(def SECTION_LABEL "a84b23ca")
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
        (objectives/get-objective anything) => objective-not-in-drafting
        (objectives/get-objective :objective-in-drafting) => objective-in-drafting
        (objectives/get-objective :objective-not-in-drafting) => objective-not-in-drafting
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
             (objectives/get-objective OBJECTIVE_ID) => {:status "open"}))
       
       (fact "retrieves drafts for an objective that is in drafting"
             (actions/retrieve-drafts OBJECTIVE_ID) => {:status ::actions/success :result :drafts}
             (provided
              (objectives/get-objective OBJECTIVE_ID) => {:status "drafting"}
               (drafts/retrieve-drafts OBJECTIVE_ID) => :drafts))
       
       (fact "can only retrieve latest draft for an objective in drafting"
             (actions/retrieve-latest-draft OBJECTIVE_ID) => {:status ::actions/objective-drafting-not-started}
             (provided
              (objectives/get-objective OBJECTIVE_ID) => {:status "open"}))

       (fact "retrieves latest draft for an objective that is in drafting"
             (actions/retrieve-latest-draft OBJECTIVE_ID) => {:status ::actions/success :result :draft}
             (provided
              (objectives/get-objective OBJECTIVE_ID) => {:status "drafting"} 
               (drafts/retrieve-latest-draft OBJECTIVE_ID) => :draft)))

(def a-draft {:entity :draft})
(def a-section {:entity :section :objective-id OBJECTIVE_ID})
(def comment-data {:comment-on-uri "/entity-uri" 
                   :comment "A comment"
                   :created-by-id USER_ID})
(def section-uri (str "/objectives/" OBJECTIVE_ID "/drafts/" DRAFT_ID "/sections/" SECTION_LABEL))
(def section-comment-data {:comment-on-uri section-uri})

(facts "about creating comments"
       (fact "can comment on an objective that is not in drafting"
             (actions/create-comment! comment-data) => {:status ::actions/success :result :the-stored-comment}
             (provided
              (storage/pg-retrieve-entity-by-uri "/entity-uri" :with-global-id) => objective-not-in-drafting
              (comments/store-comment-for! objective-not-in-drafting comment-data) => :the-stored-comment))

       (fact "cannot comment on an objective that is in drafting"
             (actions/create-comment! comment-data) => {:status ::actions/objective-drafting-started}
             (provided
              (storage/pg-retrieve-entity-by-uri "/entity-uri" :with-global-id) => objective-in-drafting))

       (fact "can comment on a draft"
             (actions/create-comment! comment-data) => {:status ::actions/success :result :the-stored-comment}
             (provided
              (storage/pg-retrieve-entity-by-uri "/entity-uri" :with-global-id) => a-draft
              (comments/store-comment-for! a-draft comment-data) => :the-stored-comment))

       (fact "can comment on a draft section with existing comments"
             (actions/create-comment! section-comment-data) => {:status ::actions/success :result :the-stored-comment}
             (provided
              (storage/pg-retrieve-entity-by-uri section-uri :with-global-id) => a-section 
              (comments/store-comment-for! a-section section-comment-data) => :the-stored-comment))

       (fact "can comment on a draft section with no previous comments"
             (actions/create-comment! section-comment-data) => {:status ::actions/success :result :the-stored-comment}
             (provided
               (storage/pg-retrieve-entity-by-uri section-uri :with-global-id) => nil 
               (drafts/get-section-labels-for-draft DRAFT_ID) => [SECTION_LABEL]
               (drafts/store-section! {:entity :section :draft-id DRAFT_ID 
                                       :section-label SECTION_LABEL}) => a-section
               (comments/store-comment-for! a-section section-comment-data) => :the-stored-comment))

       (fact "reports an error when the entity to comment on cannot be found"
             (actions/create-comment! comment-data) => {:status ::actions/entity-not-found}
             (provided
              (storage/pg-retrieve-entity-by-uri anything anything) => nil)))

(def STAR_ID 47)

(def objective-uri (str "/objectives/" OBJECTIVE_ID))
(def user-uri (str "/users/" USER_ID))

(def star-data {:objective-uri objective-uri
                :created-by-id USER_ID})

(def star-data-with-id (assoc star-data :objective-id OBJECTIVE_ID))

(facts "about toggling stars"
       (fact "star is created for an objective if none already exists"
             (actions/toggle-star! star-data) => {:status ::actions/success
                                                  :result :the-stored-star}
             (provided
              (storage/pg-retrieve-entity-by-uri objective-uri) => {:_id OBJECTIVE_ID}
              (stars/get-star objective-uri user-uri) => nil
              (stars/store-star! star-data-with-id) => :the-stored-star))

       (fact "the state of the star is toggled if the user has already starred the objective"
             (actions/toggle-star! star-data) => {:status ::actions/success
                                                  :result :the-toggled-star}
             (provided
              (storage/pg-retrieve-entity-by-uri objective-uri) => {:_id OBJECTIVE_ID}
              (stars/get-star objective-uri user-uri) => :star
              (stars/toggle-star! :star) => :the-toggled-star)))

(def QUESTION_ID 3)

(def question-uri (str "/questions/" QUESTION_ID))

(def mark-data {:question-uri question-uri
               :created-by-uri user-uri })

(facts "about marking questions"
       (fact "a mark is created if none already exists"
             (actions/mark-question! mark-data) => {:status ::actions/success
                                                    :result :the-new-mark}
             (provided
              (questions/get-question question-uri) => :a-question
              (marks/get-mark-for-question question-uri) => nil
              (marks/store-mark! (contains (assoc mark-data :active true))) => :the-new-mark))

       (fact "the state of the mark is toggled if a mark already exists for the question"
             (actions/mark-question! mark-data) => {:status ::actions/success
                                                    :result :the-new-mark}
             (provided
              (questions/get-question question-uri) => :a-question
              (marks/get-mark-for-question question-uri) => {:active true}
              (marks/store-mark! (contains (assoc mark-data :active false))) => :the-new-mark))

       (fact "a question with the given uri must exist in order to be marked"
             (actions/mark-question! mark-data) => {:status ::actions/entity-not-found}
             (provided
              (questions/get-question question-uri) => nil)))

(facts "about getting users"
       (fact "gets user with writer records, owned objectives and admin role if they exist"
             (actions/get-user-with-roles user-uri) 
             => {:status ::actions/success
                 :result {:entity :user
                          :_id USER_ID
                          :twitter-id "twitter-id"
                          :owned-objectives :stubbed-owned-objectives
                          :writer-records :stubbed-writer-records
                          :admin true}}
             (provided
               (users/retrieve-user user-uri) => {:entity :user :_id USER_ID :twitter-id "twitter-id"}
               (users/get-admin-by-twitter-id "twitter-id") => {:twitter-id "twitter-id"}
               (writers/retrieve-writers-by-user-id USER_ID) => :stubbed-writer-records 
               (objectives/get-objectives-owned-by-user-id USER_ID) => :stubbed-owned-objectives)))

(def user-uri (str "/users/" USER_ID))
(def profile-data {:name "name" :biog "biography" :user-uri user-uri})
(def updated-user (assoc {:entity :user :_id USER_ID} :profile (dissoc profile-data :user-uri)))

(facts "about updating a user with a writer profile"
       (fact "updates a user's profile"
             (actions/update-user-with-profile! profile-data) => {:status ::actions/success
                                                                  :result :the-updated-user}
             (provided
               (users/retrieve-user user-uri) => {:entity :user :_id USER_ID}
               (users/update-user! updated-user) => :the-updated-user)))

(def invitation {:objective-id OBJECTIVE_ID
                 :invited-by-id USER_ID})

(facts "about creating an invitation"
       (fact "succeeds when the associated objective is not in drafting and the inviter is an existing writer"
             (against-background
               (objectives/get-objectives-owned-by-user-id USER_ID) => [])
             (actions/create-invitation! invitation) => {:status ::actions/success
                                                         :result :stored-invitation} 
             (provided
              (objectives/get-objective OBJECTIVE_ID) => {:status "open"}
               (writers/retrieve-writers-by-user-id USER_ID) => [{:objective-id OBJECTIVE_ID}]
               (invitations/store-invitation! invitation) => :stored-invitation))

       (fact "succeeds when the associated objective is not in drafting and the inviter is the objective owner"
             (against-background
               (writers/retrieve-writers-by-user-id USER_ID) => [])
             (actions/create-invitation! invitation) => {:status ::actions/success
                                                         :result :stored-invitation} 
             (provided
              (objectives/get-objective OBJECTIVE_ID) => {:status "open"}
               (objectives/get-objectives-owned-by-user-id USER_ID) => [{:_id OBJECTIVE_ID}]
               (invitations/store-invitation! invitation) => :stored-invitation))

       (fact "returns objective-drafting-started status when the associated objective is in drafting"
             (actions/create-invitation! invitation) => {:status ::actions/objective-drafting-started} 
             (provided
              (objectives/get-objective OBJECTIVE_ID) => {:status "drafting"}))

       (fact "returns failure status when the inviter is not authorised"
             (actions/create-invitation! invitation) => {:status ::actions/failure}
             (provided
              (objectives/get-objective OBJECTIVE_ID) => {:status "open"}
               (objectives/get-objectives-owned-by-user-id USER_ID) => []
               (writers/retrieve-writers-by-user-id USER_ID) => []))

       (fact "returns a list of objective-ids a user is writer-inviter for"
             (actions/authorised-objectives-for-inviter USER_ID) => '(1 2 3 4)
             (provided
               (writers/retrieve-writers-by-user-id USER_ID) => [{:objective-id 1} {:objective-id 2}]
               (objectives/get-objectives-owned-by-user-id USER_ID) => [{:_id 3} {:_id 4}])))

(def objective {:created-by-id USER_ID :title "SOME TITLE"})
(def stored-objective (assoc objective :_id OBJECTIVE_ID))
(def invitation {:invited-by-id USER_ID
                 :objective-id OBJECTIVE_ID
                 :reason (str "Default writer as creator of this objective")
                 :writer-name "UserName"})
(def writer-data {:invitation-uuid UU_ID
                  :invitee-id USER_ID})
(def user-uri (str "/users/" USER_ID))


(facts "about creating an objective"
       (fact "when an objective is stored the creator becomes a writer for the objective"
             (actions/create-objective! objective) => {:status ::actions/success
                                                       :result stored-objective}
             (provided
               (objectives/store-objective! objective) => stored-objective
               (users/retrieve-user user-uri) => {:username "UserName"}
               (invitations/store-invitation! invitation) => {:uuid UU_ID}
               (users/update-user! (contains {:profile {:name "UserName" :biog "This profile was automatically generated for the creator of objective: SOME TITLE"}})) => {}
               (writers/create-writer writer-data) => :writer)))

(def writer-note {:note "the note content" :created-by-id USER_ID :note-on-uri ANSWER_URI})

(def entity-to-note-on {:objective-id OBJECTIVE_ID})

(facts "about creating a note"
       (fact "writers can create a note against an answer"
             (actions/create-writer-note! writer-note) => {:status ::actions/success
                                                           :result :stored-note}
             (provided
               (writer-notes/store-note-for! entity-to-note-on writer-note) => :stored-note
               (writers/retrieve-writers-by-user-id USER_ID) => [{:objective-id OBJECTIVE_ID} {:objective-id 2}]
               (storage/pg-retrieve-entity-by-uri ANSWER_URI :with-global-id) => entity-to-note-on
               (writer-notes/retrieve-note ANSWER_URI) => [])))
