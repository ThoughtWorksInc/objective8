(ns objective8.unit.http-api-test
  (:require [midje.sweet :refer :all]
            [objective8.front-end.api.http :as http-api]
            [objective8.utils :as utils]))

(def BEARER_NAME "bearer")
(def BEARER_TOKEN "token")

(background
  [(before :facts (alter-var-root #'utils/api-url (constantly "stub/api/url")))])

(background (http-api/get-api-credentials) => {"api-bearer-name"  BEARER_NAME
                                               "api-bearer-token" BEARER_TOKEN})

(facts "about API GET requests"
       (tabular
         (fact "maps http response status to API status"
               (http-api/default-get-call "/some/url") => (contains {:status ?http-api-status})
               (provided (http-api/get-request "/some/url" {}) => {:body "" :status ?http-status}))
         ?http-status ?http-api-status
         200 ::http-api/success
         404 ::http-api/not-found
         400 ::http-api/invalid-input
         403 ::http-api/forbidden
         :anything ::http-api/error))

(facts "about API POST requests"
       (tabular
         (fact "maps http response status to API status"
               (http-api/default-post-call "/some/url" {:some :data}) => (contains {:status ?http-api-status})
               (provided (http-api/post-request "/some/url" anything) => {:status ?http-status :body ""}))
         ?http-status ?http-api-status
         201 ::http-api/success
         400 ::http-api/invalid-input
         :anything ::http-api/error)

       (fact "accesses the API with the front-end credentials"
             (http-api/default-post-call "/some/url" {:some :data}) => anything
             (provided
               (http-api/post-request "/some/url"
                                      (contains
                                        {:headers (contains
                                                    {"api-bearer-name"  BEARER_NAME
                                                     "api-bearer-token" BEARER_TOKEN})})) => {:status 200 :body ""})))
(facts "about API PUT requests"
       (tabular
         (fact "maps http response status to API status"
               (http-api/default-put-call "/some/url" {:some :data}) => (contains {:status ?http-api-status})
               (provided (http-api/put-request "/some/url" anything) => {:status ?http-status :body ""}))
         ?http-status ?http-api-status
         200 ::http-api/success
         404 ::http-api/not-found
         :anything ::http-api/error)

       (fact "accesses the API with the front-end credentials"
             (http-api/default-put-call "/some/url" {:some :data}) => anything
             (provided
               (http-api/put-request "/some/url"
                                     (contains
                                       {:headers (contains
                                                   {"api-bearer-name"  BEARER_NAME
                                                    "api-bearer-token" BEARER_TOKEN})})) => {:status 200 :body ""})))

;;USERS
(def USER_ID 3)
(def USER_URI (str "/users/" USER_ID))
(def USER_AUTH_PROVIDER_USER_ID "twitter-TWITTER_ID")
(def the-user {:some                  :data
               :auth-provider-user-id USER_AUTH_PROVIDER_USER_ID
               :username              "someUsername"})

(fact "creating a user record hits the correct API endpoint"
      (http-api/create-user the-user) => :api-call-result
      (provided (http-api/default-post-call (contains "/api/v1/users") the-user) => :api-call-result))

(fact "finding a user record by auth-provider-user-id hits the correct API endpoint with credentials"
      (http-api/find-user-by-auth-provider-user-id USER_AUTH_PROVIDER_USER_ID) => :api-call-result
      (provided
        (http-api/default-get-call
          (contains (str "/api/v1/users?auth_provider_user_id=" USER_AUTH_PROVIDER_USER_ID))
          (contains {:headers (contains {"api-bearer-name"  anything
                                         "api-bearer-token" anything})})) => :api-call-result))

(fact "finding a user record by username hits the correct API endpoint with credentials"
      (http-api/find-user-by-username (:username the-user)) => :api-call-result
      (provided
        (http-api/default-get-call
          (contains (str "/api/v1/users?username=" (:username the-user)))
          (contains {:headers (contains {"api-bearer-name"  anything
                                         "api-bearer-token" anything})})) => :api-call-result))

(fact "getting a user record hits the correct API endpoint with credentials"
      (http-api/get-user USER_ID) => :api-call-result
      (provided
        (http-api/default-get-call
          (contains (str "/api/v1/users/" USER_ID))
          (contains {:headers (contains {"api-bearer-name"  anything
                                         "api-bearer-token" anything})})) => :api-call-result))

;OBJECTIVES
(def OBJECTIVE_ID 234)
(def OBJECTIVE_URI (str "/objectives/" OBJECTIVE_ID))

(def the-objective {:some :data})

(fact "creating an objective hits the correct API endpoint"
      (http-api/create-objective the-objective) => :api-call-result
      (provided
        (http-api/default-post-call
          (contains "/api/v1/objectives")
          {:some :data}) => :api-call-result))

(fact "getting an objective hits the correct API endpoint"
      (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                :result the-objective}
      (provided
        (http-api/default-get-call
          (contains (str "/api/v1/objectives/" OBJECTIVE_ID))
          (contains {:headers (contains {"api-bearer-name"  anything
                                         "api-bearer-token" anything})})) => {:status ::http-api/success
                                                                              :result {:some :data}}))

(fact "getting an objective as a signed in user hits the correct API endpoint"
      (http-api/get-objective OBJECTIVE_ID {:signed-in-id USER_ID}) => {:status ::http-api/success
                                                                        :result the-objective}
      (provided
        (http-api/default-get-call
          (contains (str "/api/v1/objectives/" OBJECTIVE_ID))
          (contains {:headers      (contains {"api-bearer-name"  anything
                                              "api-bearer-token" anything})
                     :query-params (contains {:signed-in-id USER_ID})})) => {:status ::http-api/success
                                                                             :result {:some :data}}))


(fact "getting an objective with stars-count hits the correct API endpoint with correct query parameters"
      (http-api/get-objective OBJECTIVE_ID {:with-stars-count true}) => {:status ::http-api/success
                                                                         :result the-objective}
      (provided
        (http-api/default-get-call
          (contains (str "/api/v1/objectives/" OBJECTIVE_ID))
          (contains {:headers      (contains {"api-bearer-name"  anything
                                              "api-bearer-token" anything})
                     :query-params (contains {:with-stars-count true})})) => {:status ::http-api/success
                                                                              :result {:some :data}}))


(fact "getting objectives hits the correct API endpoint"
      (http-api/get-objectives) => {:status ::http-api/success
                                    :result [the-objective]}
      (provided
        (http-api/default-get-call
          (contains (utils/api-path-for :api/get-objectives))
          (contains {:headers (contains {"api-bearer-name"  anything
                                         "api-bearer-token" anything})})) => {:status ::http-api/success
                                                                              :result [{:some :data}]}))

(fact "getting objectives as a signed-in user provides correct query parameters"
      (http-api/get-objectives {:signed-in-id USER_ID}) => {:status ::http-api/success
                                                            :result [the-objective]}
      (provided
        (http-api/default-get-call
          (contains (utils/api-path-for :api/get-objectives))
          (contains {:query-params {:user-id USER_ID}})) => {:status ::http-api/success
                                                             :result [{:some :data}]}))

(fact "getting objectives for a writer returns list of objectives when API call is successful"
      (http-api/get-objectives-for-writer USER_ID) => {:status ::http-api/success
                                                       :result [the-objective]}

      (provided
        (http-api/default-get-call
          (contains (utils/api-path-for :api/get-objectives-for-writer :id USER_ID))) => {:status ::http-api/success
                                                                                          :result [{:some :data}]}))

(fact "getting objectives for a writer returns failure status if api call is unsuccessful"
      (http-api/get-objectives-for-writer USER_ID) => {:status ::http-api/not-found}

      (provided
        (http-api/default-get-call
          (contains (utils/api-path-for :api/get-objectives-for-writer :id USER_ID))) => {:status ::http-api/not-found}))

;; COMMENTS
(def some-uri "/some/uri")
(def comment-data {:comment-on-uri some-uri :created-by-id USER_ID :comment "A comment"})

(fact "posting a comment on an entity hits the correct API endpoint"
      (http-api/post-comment comment-data) => :api-call-result
      (provided
        (http-api/default-post-call (contains "/api/v1/meta/comments") comment-data) => :api-call-result))

(fact "getting comments for an entity hits the correct API endpoint"
      (http-api/get-comments some-uri) => :api-call-result
      (provided
        (http-api/default-get-call
          (contains (utils/api-path-for :api/get-comments))
          {:query-params {:uri some-uri}}) => :api-call-result))

;; NOTES
(def note-data {:note-on-uri some-uri :created-by-id USER_ID :note "A note"})
(fact "posting a note on a question hits the correct API endpoint"
      (http-api/post-writer-note note-data) => :api-call-result
      (provided
        (http-api/default-post-call (contains "/api/v1/meta/writer-notes") note-data) => :api-call-result))

;; QUESTIONS

(def the-question {:some         :data
                   :objective-id OBJECTIVE_ID})

(def QUESTION_ID 42)

(fact "creating a question hits the correct API endpoint"
      (http-api/create-question the-question) => :api-call-result
      (provided
        (http-api/default-post-call
          (contains (str "/api/v1/objectives/" OBJECTIVE_ID "/questions"))
          the-question) => :api-call-result))

(fact "getting a question for an objective hits the correct API endpoint"
      (http-api/get-question OBJECTIVE_ID QUESTION_ID) => :api-call-result
      (provided (http-api/default-get-call (contains (str "/api/v1/objectives/" OBJECTIVE_ID
                                                          "/questions/" QUESTION_ID))) => :api-call-result))

(fact "getting all questions for an objective hits the correct API endpoint"
      (http-api/retrieve-questions OBJECTIVE_ID) => :api-call-result
      (provided
        (http-api/default-get-call (contains (str "/api/v1/objectives/" OBJECTIVE_ID
                                                  "/questions")) {:query-params {}}) => :api-call-result))

(fact "getting all questions for an objective sorted by answer count hits the correct API endpoint"
      (http-api/retrieve-questions OBJECTIVE_ID {:sorted-by "answers"}) => :api-call-result
      (provided
        (http-api/default-get-call (contains (utils/api-path-for :api/get-questions-for-objective :id OBJECTIVE_ID))
                                   {:query-params {:sorted-by "answers"}}) => :api-call-result))

(def mark-data {:question-uri "/objectives/1/questions/2" :created-by-uri "/users/2"})

(fact "marking a question hits the correct API endpoint"
      (http-api/post-mark mark-data) => :api-call-result
      (provided
        (http-api/default-post-call (contains "/api/v1/meta/marks")
                                    mark-data) => :api-call-result))

;; ANSWERS

(def the-answer {:some         :data
                 :objective-id OBJECTIVE_ID
                 :question-id  QUESTION_ID})

(fact "creating an answer hits the correct API endpoint"
      (http-api/create-answer the-answer) => :api-call-result
      (provided (http-api/default-post-call (contains (str utils/api-url "/api/v1/objectives/" OBJECTIVE_ID
                                                           "/questions/" QUESTION_ID "/answers"))
                                            the-answer) => :api-call-result))

(def QUESTION_URI (str "/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))
(fact "getting all answers for a question hits the correct API endpoint"
      (http-api/retrieve-answers QUESTION_URI) => :api-call-result
      (provided
        (http-api/default-get-call (contains (str "/api/v1/objectives/" OBJECTIVE_ID
                                                  "/questions/" QUESTION_ID "/answers")) {:query-params {}}) => :api-call-result))

(fact "getting all answers for a question sorted by up-down-votes hits the correct API endpoint"
      (http-api/retrieve-answers QUESTION_URI {:sorted-by "up-votes"}) => :api-call-result
      (provided
        (http-api/default-get-call (contains (utils/api-path-for :api/get-answers-for-question
                                                                 :id OBJECTIVE_ID :q-id QUESTION_ID))
                                   {:query-params {:sorted-by "up-votes"}}) => :api-call-result))

;; WRITERS

(def the-invitation {:some         :data
                     :objective-id OBJECTIVE_ID})

(fact "creating an invitation hits the correct API endpoint"
      (http-api/create-invitation the-invitation) => :api-call-result
      (provided
        (http-api/default-post-call (contains (str utils/api-url "/api/v1/objectives/" OBJECTIVE_ID
                                                   "/writer-invitations")) the-invitation) => :api-call-result))

(def profile-data {:name     "NAME"
                   :biog     "Biography"
                   :user-uri (str "/users/" USER_ID)})

(fact "posting a profile hits the correct API endpoint"
      (http-api/post-profile profile-data) => :api-call-result
      (provided
        (http-api/default-put-call (contains (str utils/api-url "/api/v1/users/writer-profiles")) profile-data) => :api-call-result))

(def UUID "some-long-random-string")

(fact "retrieving an invitation hits the correct API endpoint"
      (http-api/retrieve-invitation-by-uuid UUID) => :api-call-result
      (provided
        (http-api/default-get-call (contains (str utils/api-url "/api/v1/invitations?uuid=" UUID))) => :api-call-result))

(def INVITATION_ID 3)
(def the-writer {:invitation-uuid UUID
                 :invitee-id      10
                 :objective-id    OBJECTIVE_ID})

(fact "posting a writer hits the correct API endpoint"
      (http-api/post-writer the-writer) => :api-call-result
      (provided
        (http-api/default-post-call (contains (str utils/api-url "/api/v1/objectives/" OBJECTIVE_ID
                                                   "/writers")) the-writer) => :api-call-result))

(def declined-invitation {:invitation-uuid UUID
                          :invitation-id   INVITATION_ID
                          :objective-id    OBJECTIVE_ID})
(fact "declining an invitation hits the correct API endpoint"
      (http-api/decline-invitation declined-invitation) => :api-call-result
      (provided
        (http-api/default-put-call (contains (str utils/api-url "/api/v1/objectives/" OBJECTIVE_ID
                                                  "/writer-invitations/" INVITATION_ID)) declined-invitation) => :api-call-result))

(fact "getting writers for an objective hits the correct API endpoint"
      (http-api/retrieve-writers OBJECTIVE_ID) => :api-call-result
      (provided
        (http-api/default-get-call (contains (str utils/api-url "/api/v1/objectives/"
                                                  OBJECTIVE_ID "/writers"))) => :api-call-result))

;;DRAFTS

(def DRAFT_ID 876)
(def draft {:content      :markdown-as-hiccup
            :objective-id OBJECTIVE_ID
            :submitter-id USER_ID})
(def SECTION_LABEL "abcd1212")
(def section-uri (str "/objectives/" OBJECTIVE_ID "/drafts/" DRAFT_ID "/sections/" SECTION_LABEL))
(def draft-uri (str "/objectives/" OBJECTIVE_ID "/drafts/" DRAFT_ID))

(fact "posting a draft hits the correct API endpoint"
      (http-api/post-draft draft) => :api-call-result
      (provided
        (http-api/default-post-call
          (contains (str utils/api-url
                         "/api/v1/objectives/" OBJECTIVE_ID
                         "/drafts")) draft) => :api-call-result))

(fact "getting a draft for an objective hits the correct API endpoint"
      (http-api/get-draft OBJECTIVE_ID DRAFT_ID) => :api-call-result
      (provided
        (http-api/default-get-call (contains (utils/api-path-for :api/get-draft :id OBJECTIVE_ID :d-id DRAFT_ID))) => :api-call-result))

(fact "getting drafts for an objective hits the correct API endpoint"
      (http-api/get-all-drafts OBJECTIVE_ID) => :api-call-result
      (provided
        (http-api/default-get-call (contains (utils/api-path-for :api/get-drafts-for-objective :id OBJECTIVE_ID))) => :api-call-result))

(fact "getting a draft section hits the correct API endpoint"
      (http-api/get-draft-section section-uri) => :api-call-result
      (provided
        (http-api/default-get-call (contains (utils/api-path-for :api/get-section :id OBJECTIVE_ID :d-id DRAFT_ID :section-label SECTION_LABEL))) => :api-call-result))

(fact "getting draft sections hits the correct API endpoint"
      (http-api/get-draft-sections draft-uri) => :api-call-result
      (provided
        (http-api/default-get-call (contains (utils/api-path-for :api/get-sections :id OBJECTIVE_ID :d-id DRAFT_ID))) => :api-call-result))

(fact "getting annotations hits the correct API endpoint"
      (http-api/get-annotations draft-uri) => :api-call-result
      (provided
        (http-api/default-get-call (contains (utils/api-path-for :api/get-annotations :id OBJECTIVE_ID :d-id DRAFT_ID))) => :api-call-result))

;;STARS

(def star-data {:objective-id OBJECTIVE_ID :created-by-id USER_ID})

(fact "posting a star hits the correct API endpoint"
      (http-api/post-star star-data) => :api-call-result
      (provided
        (http-api/default-post-call (contains "/api/v1/meta/stars") star-data) => :api-call-result))

;;ADMIN REMOVALS

(def admin-removal-data {:removal-uri OBJECTIVE_URI :removed-by-uri USER_URI})

(fact "posting an admin removal hits the correct API endpoint"
      (http-api/post-admin-removal admin-removal-data) => :api-call-result
      (provided
        (http-api/default-post-call (contains "/api/v1/meta/admin-removals") admin-removal-data) => :api-call-result))

(fact "getting admin removals hits the correct API endpoint"
      (http-api/get-admin-removals) => :api-call-result
      (provided
        (http-api/default-get-call (contains (utils/api-path-for :api/get-admin-removals))) => :api-call-result))

;;Promoting objectives

(def promoted-objective-data {:objective-uri OBJECTIVE_URI :user-uri USER_URI})

(fact "posting a promoted objective hits the correct API endpoint"
      (http-api/post-promote-objective promoted-objective-data) => :api-call-result
      (provided
        (http-api/default-put-call (contains "/api/v1/meta/promote-objective") promoted-objective-data) => :api-call-result))

