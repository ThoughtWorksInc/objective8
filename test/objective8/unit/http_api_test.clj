(ns objective8.unit.http-api-test
  (:use org.httpkit.fake)
  (:require [org.httpkit.client :as http]
            [midje.sweet :refer :all]
            [cheshire.core :as json]
            [objective8.http-api :as http-api]
            [objective8.utils :as utils]))

(def host-url utils/host-url)

(def BEARER_NAME "bearer")
(def BEARER_TOKEN "token")

(background (http-api/get-api-credentials) => {"api-bearer-name" BEARER_NAME
                                               "api-bearer-token" BEARER_TOKEN})

(facts "about API GET requests"
       (tabular
         (fact "maps http response status to API status"
               (http-api/default-get-call "/some/url") => (contains {:status ?http-api-status})
               (provided (http-api/get-request "/some/url" {}) => {:body "" :status ?http-status}))
         ?http-status        ?http-api-status
         200                 ::http-api/success
         404                 ::http-api/not-found
         400                 ::http-api/invalid-input
         403                 ::http-api/forbidden
         :anything           ::http-api/error))

(facts "about API POST requests"
       (tabular
         (fact "maps http response status to API status"
               (http-api/default-post-call "/some/url" {:some :data}) => (contains {:status ?http-api-status})
               (provided (http-api/post-request "/some/url" anything) => {:status ?http-status :body ""}))
         ?http-status        ?http-api-status
         201                 ::http-api/success
         400                 ::http-api/invalid-input
         :anything           ::http-api/error)

       (fact "accesses the API with the front-end credentials"
             (http-api/default-post-call "/some/url" {:some :data}) => anything
             (provided
               (http-api/post-request "/some/url"
                                      (contains
                                        {:headers (contains
                                                    {"api-bearer-name" BEARER_NAME
                                                     "api-bearer-token" BEARER_TOKEN})})) => {:status 200 :body ""})))
(facts "about API PUT requests"
       (tabular
         (fact "maps http response status to API status"
               (http-api/default-put-call "/some/url" {:some :data}) => (contains {:status ?http-api-status})
               (provided (http-api/put-request "/some/url" anything) => {:status ?http-status :body ""}))
         ?http-status        ?http-api-status
         200                 ::http-api/success
         404                 ::http-api/not-found
         :anything           ::http-api/error)

       (fact "accesses the API with the front-end credentials"
             (http-api/default-put-call "/some/url" {:some :data}) => anything
             (provided
               (http-api/put-request "/some/url"
                                      (contains
                                        {:headers (contains
                                                    {"api-bearer-name" BEARER_NAME
                                                     "api-bearer-token" BEARER_TOKEN})})) => {:status 200 :body ""})))

;;USERS
(def USER_ID 3)
(def the-user {:some :data
               :twitter-id "twitter-TWITTER_ID"})

(fact "creating a user record hits the correct API endpoint"
       (http-api/create-user the-user) => :api-call-result
       (provided (http-api/default-post-call (contains "/api/v1/users") the-user) => :api-call-result))

(fact "finding a user record by twitter-id hits the correct API endpoint with credentials"
      (http-api/find-user-by-twitter-id (:twitter-id the-user)) => :api-call-result
      (provided
        (http-api/default-get-call
          (contains (str "/api/v1/users?twitter=" (:twitter-id the-user)))
          (contains {:headers (contains {"api-bearer-name" anything
                                         "api-bearer-token" anything})})) => :api-call-result))

(fact "getting a user record hits the correct API endpoint with credentials"
      (http-api/get-user USER_ID) => :api-call-result
      (provided
       (http-api/default-get-call
         (contains (str "/api/v1/users/" USER_ID))
         (contains {:headers (contains {"api-bearer-name" anything
                                        "api-bearer-token" anything})})) => :api-call-result))

;OBJECTIVES
(def OBJECTIVE_ID 234)

(def the-objective {:some :data
                    :end-date (utils/string->date-time "2015-01-31")})

(fact "creating an objective hits the correct API endpoint"
      (http-api/create-objective the-objective) => :api-call-result
      (provided
        (http-api/default-post-call
          (contains "/api/v1/objectives")
          {:some :data
           :end-date "2015-01-31T00:00:00.000Z"}) => :api-call-result))

(fact "getting an objective hits the correct API endpoint"
      (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                :result the-objective}
      (provided
        (http-api/default-get-call
          (contains (str "/api/v1/objectives/" OBJECTIVE_ID))
          (contains {:headers (contains {"api-bearer-name" anything
                                         "api-bearer-token" anything})})) => {:status ::http-api/success
                                                                              :result {:some :data
                                                                                       :end-date "2015-01-31T00:00:00.000Z" }}))

(fact "getting an objective as a signed in user hits the correct API endpoint"
      (http-api/get-objective OBJECTIVE_ID {:signed-in-id USER_ID}) => {:status ::http-api/success
                                                                        :result the-objective}
      (provided
        (http-api/default-get-call
          (contains (str "/api/v1/objectives/" OBJECTIVE_ID))
          (contains {:headers (contains {"api-bearer-name" anything
                                         "api-bearer-token" anything})
                     :query-params (contains {:signed-in-id USER_ID})})) => {:status ::http-api/success
                                                                             :result {:some :data
                                                                                      :end-date "2015-01-31T00:00:00.000Z" }}))

(fact "getting objectives hits the correct API endpoint"
      (http-api/get-objectives) => {:status ::http-api/success
                                    :result [the-objective]} 
      (provided
        (http-api/default-get-call
          (contains (utils/path-for :api/get-objectives))
          (contains {:headers (contains {"api-bearer-name" anything
                                         "api-bearer-token" anything})})) => {:status ::http-api/success
                                                                              :result [{:some :data
                                                                                        :end-date "2015-01-31T00:00:00.000Z"}]}))

(fact "getting objectives as a signed-in user provides correct query parameters"
      (http-api/get-objectives {:signed-in-id USER_ID}) => {:status ::http-api/success
                                                            :result [the-objective]}
      (provided
        (http-api/default-get-call
          (contains (utils/path-for :api/get-objectives))
          (contains {:query-params {:user-id USER_ID}})) => {:status ::http-api/success
                                                             :result [{:some :data
                                                                       :end-date "2015-01-31T00:00:00.000Z"}]}))

;; COMMENTS
(def some-uri "/some/uri")
(def comment-data {:comment-on-uri some-uri :created-by-id USER_ID :comment "A comment"})

(fact "posting a comment on an entity hits the correct API endpoint"
      (http-api/post-comment comment-data) => :api-call-result
      (provided
       (http-api/default-post-call (contains "/api/v1/meta/comments") comment-data) => :api-call-result))

(fact "getting comments for an entity hits the correct API endpoint"
      (http-api/get-comments some-uri)=> :api-call-result
      (provided
       (http-api/default-get-call
         (contains (utils/path-for :api/get-comments))
         {:query-params {:uri some-uri}}) => :api-call-result))

;; QUESTIONS

(def the-question {:some :data
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
                                                  "/questions"))) => :api-call-result))
;; ANSWERS

(def the-answer {:some :data
                 :objective-id OBJECTIVE_ID
                 :question-id QUESTION_ID})

(fact "creating an answer hits the correct API endpoint"
      (http-api/create-answer the-answer) => :api-call-result
      (provided (http-api/default-post-call (contains (str host-url "/api/v1/objectives/" OBJECTIVE_ID
                                                             "/questions/" QUESTION_ID "/answers"))
                  the-answer) => :api-call-result))

(fact "getting all answers for a question hits the correct API endpoint"
      (http-api/retrieve-answers OBJECTIVE_ID QUESTION_ID) => :api-call-result
      (provided
        (http-api/default-get-call (contains (str "/api/v1/objectives/" OBJECTIVE_ID
                                                  "/questions/" QUESTION_ID "/answers"))) => :api-call-result))

;; WRITERS

(def the-invitation {:some :data
                     :objective-id OBJECTIVE_ID})

(fact "creating an invitation hits the correct API endpoint"
      (http-api/create-invitation the-invitation) => :api-call-result
      (provided 
        (http-api/default-post-call (contains (str host-url "/api/v1/objectives/" OBJECTIVE_ID 
                                                     "/writer-invitations")) the-invitation) => :api-call-result))

;; INVITATIONS

(def UUID "some-long-random-string")

(fact "retrieving an invitation hits the correct API endpoint"
      (http-api/retrieve-invitation-by-uuid UUID) => :api-call-result
      (provided
        (http-api/default-get-call (contains (str host-url "/api/v1/invitations?uuid=" UUID))) => :api-call-result))

(def INVITATION_ID 3)
(def the-candidate-writer {:invitation-uuid UUID
                           :invitee-id 10
                           :objective-id OBJECTIVE_ID})

(fact "posting a candidate writer hits the correct API endpoint"
      (http-api/post-candidate-writer the-candidate-writer) => :api-call-result
      (provided
        (http-api/default-post-call (contains (str host-url "/api/v1/objectives/" OBJECTIVE_ID 
                                                     "/candidate-writers")) the-candidate-writer) => :api-call-result))

(def declined-invitation {:invitation-uuid UUID
                          :invitation-id INVITATION_ID
                          :objective-id OBJECTIVE_ID})
(fact "declining an invitation hits the correct API endpoint"
      (http-api/decline-invitation declined-invitation) => :api-call-result
      (provided
        (http-api/default-put-call (contains (str host-url "/api/v1/objectives/" OBJECTIVE_ID
                                                  "/writer-invitations/" INVITATION_ID)) declined-invitation) => :api-call-result))

;;CANDIDATES

(fact "getting candidates for an objective hits the correct API endpoint"
      (http-api/retrieve-candidates OBJECTIVE_ID) => :api-call-result
      (provided
        (http-api/default-get-call (contains (str host-url "/api/v1/objectives/"
                                                  OBJECTIVE_ID "/candidate-writers"))) => :api-call-result))

;;DRAFTS

(def DRAFT_ID 876)
(def draft {:content :markdown-as-hiccup
            :objective-id OBJECTIVE_ID
            :submitter-id USER_ID})
(fact "posting a draft hits the correct API endpoint"
      (http-api/post-draft draft) => :api-call-result
      (provided
       (http-api/default-post-call
         (contains (str host-url
                        "/api/v1/objectives/" OBJECTIVE_ID
                        "/drafts")) draft) => :api-call-result))

(fact "getting a draft for an objective hits the correct API endpoint"
      (http-api/get-draft OBJECTIVE_ID DRAFT_ID) => :api-call-result
      (provided
        (http-api/default-get-call (contains (utils/path-for :api/get-draft :id OBJECTIVE_ID :d-id DRAFT_ID))) => :api-call-result))

(fact "getting drafts for an objective hits the correct API endpoint"
      (http-api/get-all-drafts OBJECTIVE_ID) => :api-call-result
      (provided
        (http-api/default-get-call (contains (utils/path-for :api/get-drafts-for-objective :id OBJECTIVE_ID))) => :api-call-result))

;;STARS

(def star-data {:objective-id OBJECTIVE_ID :created-by-id USER_ID})

(fact "posting a star hits the correct API endpoint"
      (http-api/post-star star-data) => :api-call-result
      (provided
        (http-api/default-post-call (contains "/api/v1/meta/stars") star-data) => :api-call-result))
