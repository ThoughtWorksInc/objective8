(ns objective8.http-api-test
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

(facts "about retrieving information from the API"
       (tabular
         (fact "maps http response status to API status"
               (http-api/default-get-call "/some/url") => (contains {:status ?http-api-status})
               (provided (http-api/get-request "/some/url" {}) => {:body "" :status ?http-status}))
         ?http-status        ?http-api-status
         200                 ::http-api/success
         404                 ::http-api/not-found
         400                 ::http-api/invalid-input
         :anything           ::http-api/error))

(facts "about posting information to the API"
       (tabular
         (fact "maps http response status to API status"
               (http-api/default-create-call "/some/url" {:some :data}) => (contains {:status ?http-api-status})
               (provided (http-api/post-request "/some/url" anything) => {:status ?http-status :body ""}))
         ?http-status        ?http-api-status
         201                 ::http-api/success
         400                 ::http-api/invalid-input
         :anything           ::http-api/error)

       (fact "accesses the API with the front-end credentials"
             (http-api/default-create-call "/some/url" {:some :data}) => anything
             (provided
               (http-api/post-request "/some/url"
                                      (contains
                                        {:headers (contains
                                                    {"api-bearer-name" BEARER_NAME
                                                     "api-bearer-token" BEARER_TOKEN})})) => {:status 200 :body ""})))

;USERS
(def the-user {:some :data
               :twitter-id "twitter-TWITTER_ID"})

(fact "creating a user record hits the correct API endpoint"
       (http-api/create-user the-user) => :api-call-result
       (provided (http-api/default-create-call (contains "/api/v1/users") the-user) => :api-call-result))

(fact "finding a user record hits the correct API endpoint with credentials"
      (http-api/find-user-by-twitter-id (:twitter-id the-user)) => :api-call-result
      (provided
        (http-api/default-get-call
          (contains (str "/api/v1/users?twitter=" (:twitter-id the-user)))
          (contains {:headers (contains {"api-bearer-name" anything
                                         "api-bearer-token" anything})})) => :api-call-result))

;OBJECTIVES
(def OBJECTIVE_ID 234)

(def the-objective {:some :data
                    :end-date (utils/string->date-time "2015-01-31")})

(fact "creating an objective hits the correct API endpoint"
      (http-api/create-objective the-objective) => :api-call-result
      (provided
        (http-api/default-create-call
          (contains "/api/v1/objectives")
          {:some :data
           :end-date "2015-01-31T00:00:00.000Z"}) => :api-call-result))

(fact "getting an objective hits the correct API endpoint"
      (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                :result the-objective}
      (provided
        (http-api/default-get-call
          (contains (str "/api/v1/objectives/" OBJECTIVE_ID))) => {:status ::http-api/success
                                                                   :result {:some :data
                                                                            :end-date "2015-01-31T00:00:00.000Z" }}))

;; COMMENTS

(fact "creating a comment hits the correct API endpoint"
      (http-api/create-comment {:some :data}) => :api-call-result
      (provided
        (http-api/default-create-call (contains "/api/v1/comments") {:some :data}) => :api-call-result))


(fact "retrieving comments for an objective hits the correct API endpoint"
      (http-api/retrieve-comments OBJECTIVE_ID) => :api-call-result
      (provided
        (http-api/default-get-call (contains (str "/api/v1/objectives/" OBJECTIVE_ID "/comments"))) => :api-call-result))

;; QUESTIONS

(def the-question {:some :data
                   :objective-id OBJECTIVE_ID})

(def QUESTION_ID 42)

(fact "creating a question hits the correct API endpoint"
      (http-api/create-question the-question) => :api-call-result
      (provided
        (http-api/default-create-call
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
      (provided (http-api/default-create-call (contains (str host-url "/api/v1/objectives/" OBJECTIVE_ID
                                                             "/questions/" QUESTION_ID "/answers"))
                  the-answer) => :api-call-result))

(fact "getting all answers for a question hits the correct API endpoint"
      (http-api/retrieve-answers OBJECTIVE_ID QUESTION_ID) => :api-call-result
      (provided
        (http-api/default-get-call (contains (str "/api/v1/objectives/" OBJECTIVE_ID
                                                  "/questions/" QUESTION_ID "/answers"))) => :api-call-result))

;; WRITERS

(def the-invited-writer {:some :data
                         :objective-id OBJECTIVE_ID})

(fact "inviting a writer hits the correct API endpoint"
      (http-api/invite-writer the-invited-writer) => :api-call-result
      (provided 
        (http-api/default-create-call (contains (str host-url "/api/v1/objectives/" OBJECTIVE_ID 
                                                     "/writers/invited")) the-invited-writer) => :api-call-result))
