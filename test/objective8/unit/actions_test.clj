(ns objective8.unit.actions-test
  (:require [midje.sweet :refer :all]
            [objective8.config :as config]
            [objective8.back-end.actions :as actions]
            [objective8.back-end.domain.up-down-votes :as up-down-votes]
            [objective8.back-end.domain.drafts :as drafts]
            [objective8.back-end.domain.objectives :as objectives]
            [objective8.back-end.domain.comments :as comments]
            [objective8.back-end.domain.users :as users]
            [objective8.back-end.domain.writers :as writers]
            [objective8.back-end.domain.invitations :as invitations]
            [objective8.back-end.domain.stars :as stars]
            [objective8.back-end.domain.questions :as questions]
            [objective8.back-end.domain.answers :as answers]
            [objective8.back-end.domain.marks :as marks]
            [objective8.back-end.domain.admin-removals :as admin-removals]
            [objective8.back-end.domain.activities :as activities]
            [objective8.back-end.domain.writer-notes :as writer-notes]
            [objective8.back-end.storage.storage :as storage]))

(def GLOBAL_ID 6)
(def USER_ID 2)
(def USER_URI (str "/users/" USER_ID))
(def VOTE_ID 5)
(def OBJECTIVE_ID 1)
(def QUESTION_ID 2)
(def QUESTION_URI (str "/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))
(def ANSWER_ID 3)
(def ANSWER_URI (str "/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID "/answers/" ANSWER_ID))
(def INVITATION_ID 7)
(def UU_ID "875678950430596859403-uuid")
(def DRAFT_ID 8)
(def DRAFT_URI (str "/objectives/" OBJECTIVE_ID "/drafts/" DRAFT_ID))
(def SECTION_LABEL "a84b23ca")
(def an-answer {:global-id GLOBAL_ID})
(def vote-data {:vote-on-uri   :entity-uri
                :created-by-id USER_ID
                :vote-type     :up})

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

(def an-objective {:entity :objective})

(facts "about allowing or disallowing voting"
       (against-background
         (up-down-votes/get-vote anything anything) => nil
         (objectives/get-objective anything) => an-objective
         (storage/pg-retrieve-entity-by-global-id :an-objective-global-id) => an-objective)

       (fact "the same user cannot vote twice on the same entity"
             (actions/allowed-to-vote? an-objective :vote-data) => falsey
             (provided
               (up-down-votes/get-vote anything anything) => :a-vote)))

(def a-draft {:entity :draft})
(def a-section {:entity :section :objective-id OBJECTIVE_ID})
(def comment-data {:comment-on-uri "/entity-uri"
                   :comment        "A comment"
                   :created-by-id  USER_ID})
(def COMMENT_ID)
(def section-uri (str "/objectives/" OBJECTIVE_ID "/drafts/" DRAFT_ID "/sections/" SECTION_LABEL))
(def reason-type "expand")
(def section-comment-data {:comment-on-uri section-uri :reason reason-type})


(facts "about creating comments"
       (fact "can comment on an objective"
             (actions/create-comment! comment-data) => {:status ::actions/success :result :the-stored-comment}
             (provided
               (storage/pg-retrieve-entity-by-uri "/entity-uri" :with-global-id) => an-objective
               (comments/store-comment-for! an-objective comment-data) => :the-stored-comment))

       (fact "can comment on a draft"
             (actions/create-comment! comment-data) => {:status ::actions/success :result :the-stored-comment}
             (provided
               (storage/pg-retrieve-entity-by-uri "/entity-uri" :with-global-id) => a-draft
               (comments/store-comment-for! a-draft comment-data) => :the-stored-comment))

       (fact "can comment on a draft section with existing comments"
             (actions/create-comment! section-comment-data) => {:status ::actions/success :result {:_id COMMENT_ID :reason reason-type}}
             (provided
               (storage/pg-retrieve-entity-by-uri section-uri :with-global-id) => a-section
               (comments/store-comment-for! a-section section-comment-data) => {:_id COMMENT_ID}
               (comments/store-reason! {:reason reason-type :comment-id COMMENT_ID}) => {:reason reason-type}))

       (fact "can comment on a draft section with no previous comments"
             (actions/create-comment! section-comment-data) => {:status ::actions/success :result {:_id COMMENT_ID :reason reason-type}}
             (provided
               (storage/pg-retrieve-entity-by-uri section-uri :with-global-id) => nil
               (drafts/get-section-labels-for-draft-uri DRAFT_URI) => [SECTION_LABEL]
               (drafts/store-section! {:entity        :section :draft-id DRAFT_ID :objective-id OBJECTIVE_ID
                                       :section-label SECTION_LABEL}) => a-section
               (comments/store-comment-for! a-section section-comment-data) => {:_id COMMENT_ID}
               (comments/store-reason! {:reason reason-type :comment-id COMMENT_ID}) => {:reason reason-type}))

       (fact "reports an error when the entity to comment on cannot be found"
             (actions/create-comment! comment-data) => {:status ::actions/entity-not-found}
             (provided
               (storage/pg-retrieve-entity-by-uri anything anything) => nil)))

(facts "about retrieving annotations for a draft"
       (fact "annotations are retrieved with the annotated section"
             (actions/get-annotations-for-draft DRAFT_URI) => {:status ::actions/success
                                                               :result [{:section      :some-hiccup
                                                                         :uri          :section-uri
                                                                         :objective-id OBJECTIVE_ID
                                                                         :comments     [:annotation]}]}

             (provided
               (drafts/get-annotated-sections-with-section-content DRAFT_URI) => [{:uri          :section-uri
                                                                                   :objective-id OBJECTIVE_ID
                                                                                   :section      :some-hiccup}]
               (comments/get-comments :section-uri {}) => [:annotation])))

(def STAR_ID 47)

(def OBJECTIVE-URI (str "/objectives/" OBJECTIVE_ID))
(def user-uri (str "/users/" USER_ID))

(def star-data {:objective-uri OBJECTIVE-URI
                :created-by-id USER_ID})

(def star-data-with-id (assoc star-data :objective-id OBJECTIVE_ID))

(facts "about toggling stars"
       (fact "star is created for an objective if none already exists"
             (actions/toggle-star! star-data) => {:status ::actions/success
                                                  :result :the-stored-star}
             (provided
               (storage/pg-retrieve-entity-by-uri OBJECTIVE-URI) => {:_id OBJECTIVE_ID}
               (stars/get-star OBJECTIVE-URI user-uri) => nil
               (stars/store-star! star-data-with-id) => :the-stored-star))

       (fact "the state of the star is toggled if the user has already starred the objective"
             (actions/toggle-star! star-data) => {:status ::actions/success
                                                  :result :the-toggled-star}
             (provided
               (storage/pg-retrieve-entity-by-uri OBJECTIVE-URI) => {:_id OBJECTIVE_ID}
               (stars/get-star OBJECTIVE-URI user-uri) => :star
               (stars/toggle-star! :star) => :the-toggled-star)))

(def mark-data {:question-uri   QUESTION_URI
                :created-by-uri user-uri})

(def question {:objective-id OBJECTIVE_ID :created-by-id USER_ID :uri QUESTION_URI :question "Sample question"})
(def stored-question (assoc question :_id QUESTION_ID))
(def retrieved-question (assoc stored-question :username "UserName"))

(facts "about creating questions"
       (fact "A question can be created"
             (actions/create-question! question) => {:status ::actions/success :result stored-question}
             (provided
               (objectives/get-objective OBJECTIVE_ID) => :an-objective
               (writers/retrieve-writer-for-objective USER_ID OBJECTIVE_ID) => nil
               (questions/store-question! question) => stored-question
               (questions/get-question QUESTION_URI) => retrieved-question
               (activities/store-activity! retrieved-question) => anything)))

(facts "about marking questions"
       (fact "a mark is created if none already exists"
             (actions/mark-question! mark-data) => {:status ::actions/success
                                                    :result :the-new-mark}
             (provided
               (questions/get-question QUESTION_URI) => :a-question
               (marks/get-mark-for-question QUESTION_URI) => nil
               (marks/store-mark! (contains (assoc mark-data :active true))) => :the-new-mark))

       (fact "the state of the mark is toggled if a mark already exists for the question"
             (actions/mark-question! mark-data) => {:status ::actions/success
                                                    :result :the-new-mark}
             (provided
               (questions/get-question QUESTION_URI) => :a-question
               (marks/get-mark-for-question QUESTION_URI) => {:active true}
               (marks/store-mark! (contains (assoc mark-data :active false))) => :the-new-mark))

       (fact "a question with the given uri must exist in order to be marked"
             (actions/mark-question! mark-data) => {:status ::actions/entity-not-found}
             (provided
               (questions/get-question QUESTION_URI) => nil)))

(facts "about getting users"
       (fact "gets user with writer records, owned objectives and admin role if they exist"
             (actions/get-user-with-roles user-uri)
             => {:status ::actions/success
                 :result {:entity                :user
                          :_id                   USER_ID
                          :auth-provider-user-id "twitter-id"
                          :owned-objectives      :stubbed-owned-objectives
                          :writer-records        :stubbed-writer-records
                          :admin                 true}}
             (provided
               (users/retrieve-user user-uri) => {:entity :user :_id USER_ID :auth-provider-user-id "twitter-id"}
               (users/get-admin-by-auth-provider-user-id "twitter-id") => {:auth-provider-user-id "twitter-id"}
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

(def answer {:objective-id OBJECTIVE_ID :question-id QUESTION_ID})


(facts "about creating an answer"
       (fact "succeeds when the question exists"
             (actions/create-answer! answer) => {:status ::actions/success
                                                 :result :stored-answer}
             (provided
               (questions/get-question QUESTION_URI) => :a-question
               (answers/store-answer! answer) => :stored-answer))

       (fact "returns entity-not-found status when the associated question doesn't exist"
             (actions/create-answer! answer) => {:status ::actions/entity-not-found}
             (provided
               (questions/get-question QUESTION_URI) => nil))

       (fact "returns failure status when storing the answer fails"
             (actions/create-answer! answer) => {:status ::actions/failure}
             (provided
               (questions/get-question QUESTION_URI) => :a-question
               (answers/store-answer! answer) => nil)))

(def invitation {:objective-id  OBJECTIVE_ID
                 :invited-by-id USER_ID})


(facts "about creating an invitation"
       (fact "succeeds when the inviter is an existing writer"
             (against-background
               (objectives/get-objectives-owned-by-user-id USER_ID) => [])
             (actions/create-invitation! invitation) => {:status ::actions/success
                                                         :result :stored-invitation}
             (provided
               (objectives/get-objective OBJECTIVE_ID) => :an-objective
               (writers/retrieve-writers-by-user-id USER_ID) => [{:objective-id OBJECTIVE_ID}]
               (invitations/store-invitation! invitation) => :stored-invitation))

       (fact "succeeds when the inviter is the objective owner"
             (against-background
               (writers/retrieve-writers-by-user-id USER_ID) => [])
             (actions/create-invitation! invitation) => {:status ::actions/success
                                                         :result :stored-invitation}
             (provided
               (objectives/get-objective OBJECTIVE_ID) => :an-objective
               (objectives/get-objectives-owned-by-user-id USER_ID) => [{:_id OBJECTIVE_ID}]
               (invitations/store-invitation! invitation) => :stored-invitation))

       (fact "returns failure status when the inviter is not authorised"
             (actions/create-invitation! invitation) => {:status ::actions/failure}
             (provided
               (objectives/get-objective OBJECTIVE_ID) => :an-objective
               (objectives/get-objectives-owned-by-user-id USER_ID) => []
               (writers/retrieve-writers-by-user-id USER_ID) => []))

       (fact "returns a list of objective-ids a user is writer-inviter for"
             (actions/authorised-objectives-for-inviter USER_ID) => '(1 2 3 4)
             (provided
               (writers/retrieve-writers-by-user-id USER_ID) => [{:objective-id 1} {:objective-id 2}]
               (objectives/get-objectives-owned-by-user-id USER_ID) => [{:_id 3} {:_id 4}])))

(def objective {:created-by-id USER_ID :title "SOME TITLE"})
(def stored-objective (assoc objective :_id OBJECTIVE_ID))
(def retrieved-objective (assoc stored-objective :username "UserName"))
(def invitation {:invited-by-id USER_ID
                 :objective-id  OBJECTIVE_ID
                 :reason        (str "Default writer as creator of this objective")
                 :writer-name   "UserName"})
(def writer-data {:invitation-uuid UU_ID
                  :invitee-id      USER_ID})
(def user-uri (str "/users/" USER_ID))


(facts "about creating an objective"
       (fact "when an objective is stored the creator becomes a writer for the objective"
             (actions/create-objective! objective) => {:status ::actions/success
                                                       :result stored-objective}
             (provided
               (objectives/store-objective! objective) => stored-objective
               (objectives/get-objective OBJECTIVE_ID) => retrieved-objective
               (activities/store-activity! retrieved-objective) => anything
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

(def admin-removal {:removal-uri    OBJECTIVE-URI
                    :removed-by-uri USER_URI})
(fact "admin-removals"
      (actions/create-admin-removal! admin-removal) => {:status ::actions/success
                                                        :result :stored-admin-removal}
      (provided
        (users/retrieve-user USER_URI) => {:auth-provider-user-id "twitter-123456"}
        (users/get-admin-by-auth-provider-user-id "twitter-123456") => {:auth-provider-user-id "twitter-123456"}
        (storage/pg-retrieve-entity-by-uri OBJECTIVE-URI :with-global-id) => {:_id OBJECTIVE_ID}
        (objectives/admin-remove-objective! {:_id OBJECTIVE_ID}) => {:_id OBJECTIVE_ID}
        (admin-removals/store-admin-removal! admin-removal) => :stored-admin-removal))

(def promoted-objective-data {:objective-uri OBJECTIVE-URI :promoted-by USER_URI})

(facts "about promoting objectives"
       (fact "does promote an existing objective when signed in as an admin"
             (actions/create-promote-objective! promoted-objective-data) => {:status ::actions/success
                                                                         :result     OBJECTIVE_ID}
             (provided
               (users/retrieve-user USER_URI) => {:auth-provider-user-id "twitter-123456"}
               (users/get-admin-by-auth-provider-user-id "twitter-123456") => {:auth-provider-user-id "twitter-123456"}
               (storage/pg-retrieve-entity-by-uri OBJECTIVE-URI :with-global-id) => {:_id OBJECTIVE_ID}
               (objectives/toggle-promoted-status! {:_id OBJECTIVE_ID}) => OBJECTIVE_ID))

       (fact "unable to promote objective if user is not signed in"
             (actions/create-promote-objective! promoted-objective-data) => {:status ::actions/entity-not-found}
             (provided
               (users/retrieve-user USER_URI) => nil))

       (fact "unable to promote objective if user is not an admin"
             (actions/create-promote-objective! promoted-objective-data) => {:status ::actions/forbidden}
             (provided
               (users/retrieve-user USER_URI) => {:auth-provider-user-id "twitter-123456"}
               (users/get-admin-by-auth-provider-user-id "twitter-123456") => nil))

       (fact "unable to promote objective if objective n'existe pas"
             (actions/create-promote-objective! promoted-objective-data) => {:status ::actions/entity-not-found}
             (provided
               (users/retrieve-user USER_URI) => {:auth-provider-user-id "twitter-123456"}
               (users/get-admin-by-auth-provider-user-id "twitter-123456") => {:auth-provider-user-id "twitter-123456"}
               (storage/pg-retrieve-entity-by-uri OBJECTIVE-URI :with-global-id) => nil
               )))
