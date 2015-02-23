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

(def request-with-bearer-token (contains {:headers (contains {"api-bearer-token" BEARER_TOKEN
                                                              "api-bearer-name" BEARER_NAME})}))

(background (http-api/get-api-credentials) => {"api-bearer-name" BEARER_NAME
                                               "api-bearer-token" BEARER_TOKEN})

;USERS
(def USER_ID 1)
(def the-user {:twitter-id "twitter-TWITTER_ID"
               :email-address "blah@blah.com"})

(def the-stored-user (into the-user {:_id USER_ID}))

(facts "about creating user records"
       (fact "creating a user record requires credentials"
             (http-api/create-user the-user) => {:status ::http-api/success
                                                 :result the-stored-user} 
             (provided (http-api/post-request (contains "/api/v1/users")
                                              request-with-bearer-token)
                       => {:status 201
                           :headers {"Content-Type" "application/json"}
                           :body (json/generate-string the-stored-user)}))

       (fact "returns api-failure when post fails"
             (http-api/create-user the-user) => {:status ::http-api/error}
             (provided (http-api/post-request anything anything)
                       => {:status 500})))

(facts "about finding user records"
       (fact "finding a user record requires credentials"
             (http-api/find-user-by-twitter-id (:twitter-id the-user)) => {:status ::http-api/success
                                                                           :result the-stored-user} 
             (provided (http-api/get-request (contains (str "/api/v1/users?twitter=" (:twitter-id the-user)))
                                             request-with-bearer-token)
                       => {:status 200
                           :headers {"Content-Type" "application/json"}
                           :body (json/generate-string the-stored-user)})))

;OBJECTIVES
(def OBJECTIVE_ID 234)

(def the-objective {:title "My Objective"
                    :goals "To rock out, All day"
                    :description "I like cake"
                    :end-date "2015-01-31"
                    :username "my username"})

(def the-stored-objective (into the-objective {:_id OBJECTIVE_ID}))

(facts "about posting objectives"
       (fact "returns a stored objective when post succeeds"
             (http-api/create-objective the-objective) => {:status ::http-api/success
                                                           :result the-stored-objective} 
             (provided (http-api/post-request (contains "/api/v1/objectives")
                                              request-with-bearer-token)
                       => {:status 201
                           :headers {"Content-Type" "application/json"}
                           :body (json/generate-string the-stored-objective)}))

       (fact "returns api-failure when post fails"
             (http-api/create-objective the-objective) => {:status ::http-api/error}
             (provided (http-api/post-request anything anything) => {:status 500})))

(facts "about getting objectives"
       (fact "returns a stored objective when one exists with given id"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID)
                              {:status 200
                               :headers {"Content-Type" "application/json"}
                               :body (json/generate-string
                                       {:_id OBJECTIVE_ID
                                        :title "Objective title"
                                        :goals "Objective goals"
                                        :description "Objective description"
                                        :end-date "2015-01-31T00:00:00.000Z"})}]
               (http-api/get-objective OBJECTIVE_ID))
             => (contains {:status ::http-api/success
                           :result (contains {:_id OBJECTIVE_ID
                                              :title "Objective title"
                                              :goals "Objective goals"
                                              :description "Objective description"
                                              :end-date (utils/time-string->date-time "2015-01-31T00:00:00.000Z")})}))

       (fact "returns 404 when no objective found"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID)
                              {:status 404}]
               (http-api/get-objective OBJECTIVE_ID))
             => (contains {:status ::http-api/not-found})))

;; COMMENTS

(def the-comment {:comment "The comment"
                  :objective-id OBJECTIVE_ID
                  :username "my username"})

(def the-stored-comment (into the-comment {:_id 1}))

(facts "about posting comments"
       (fact "returns a stored comment when post succeeds"
             (http-api/create-comment the-comment) => {:status ::http-api/success :result the-stored-comment} 
             (provided (http-api/post-request (contains "/api/v1/comments")
                                              request-with-bearer-token)
                       =>{:status 201
                          :headers {"Content-Type" "application/json"}
                          :body (json/generate-string the-stored-comment)}))

       (fact "returns api-failure when post fails"
             (http-api/create-comment the-comment) => {:status ::http-api/error} 
             (provided (http-api/post-request anything anything) => {:status 500})))

(facts "about retrieving comments"
       (fact "returns a list of comments for an objective"
             (with-fake-http [(str host-url "/api/v1/objectives/1/comments") {:status 200
                                                                              :headers {"Content-Type" "application/json"}
                                                                              :body (json/generate-string [the-stored-comment])}]
               (http-api/retrieve-comments 1))
             => {:status ::http-api/success
                 :result [the-stored-comment]}))

;; QUESTIONS

(def the-question {:question "The meaning of life?"
                   :objective-id OBJECTIVE_ID
                   :created-by USER_ID})

(def QUESTION_ID 42)

(def the-stored-question (into the-question {:_id QUESTION_ID}))

(facts "about posting questions"
       (fact "returns a stored question when post succeeds"
             (http-api/create-question the-question) => {:status ::http-api/success 
                                                         :result the-stored-question}
             (provided (http-api/post-request (contains (str "/api/v1/objectives/" OBJECTIVE_ID "/questions"))
                                              request-with-bearer-token)
                       => {:status 201
                           :headers {"Content-Type" "application/json"}
                           :body (json/generate-string the-stored-question)}))

       (fact "returns api-failure when post fails"
             (http-api/create-question the-question) => {:status ::http-api/error}
             (provided (http-api/post-request anything anything) => {:status 500})))

(facts "about getting questions"
       (fact "returns a stored question when one exists with a given id"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID) {:status 200
                                                                                                           :headers {"Content-Type" "application/json"}
                                                                                                           :body (json/generate-string the-stored-question)}]
               (http-api/get-question OBJECTIVE_ID QUESTION_ID))
             => {:status ::http-api/success
                 :result the-stored-question})

       (fact "returns :objective8.http-api/error when request fails"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID) {:status 500}]
               (http-api/get-question OBJECTIVE_ID QUESTION_ID))
             => {:status ::http-api/error})

       (fact "returns :objective8.http-api/not-found when no question with that id exists"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID) {:status 404}]
               (http-api/get-question OBJECTIVE_ID QUESTION_ID))
             => {:status ::http-api/not-found}))

(facts "about retrieving questions" 
       (fact "returns a list of questions for a given objective"
             (with-fake-http [(str host-url "/api/v1/objectives/"
                                   OBJECTIVE_ID "/questions") {:status 200
                                                               :headers {"Content-Type" "application/json"}
                                                               :body (json/generate-string [the-stored-question])}]
               (http-api/retrieve-questions OBJECTIVE_ID)) => {:status ::http-api/success
                                                               :result [the-stored-question]}))
;; ANSWERS

(def the-answer {:answer "The answer"
                 :objective-id OBJECTIVE_ID
                 :question-id QUESTION_ID
                 :created-by-id USER_ID})
(def the-stored-answer (into the-answer {:_id 3}))

(facts "about posting answers"
       (fact "returns a stored answer when post succeeds"
             (http-api/create-answer the-answer) => {:status ::http-api/success
                                                     :result the-stored-answer}
             (provided (http-api/post-request (contains (str host-url "/api/v1/objectives/" OBJECTIVE_ID
                                                             "/questions/" QUESTION_ID "/answers"))
                                              request-with-bearer-token)
                       => {:status 201
                           :headers {"Content-Type" "application/json"}
                           :body (json/generate-string the-stored-answer)})) 

       (fact "returns :objective8.http-api/invalid when request is invalid"
             (http-api/create-answer the-answer) => {:status ::http-api/invalid-input}
             (provided (http-api/post-request anything anything) => {:status 400}))

       (fact "returns :objective8.http-api/error when request fails"
             (http-api/create-answer the-answer) => {:status ::http-api/error}
             (provided (http-api/post-request anything anything) => {:status 500})))

(facts "about retrieving answers" 
       (fact "returns a list of answers for a given objective and question"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID 
                                   "/questions/" QUESTION_ID "/answers") {:status 200
                                                                          :headers {"Content-Type" "application/json"}
                                                                          :body (json/generate-string [the-stored-answer])}]
               (http-api/retrieve-answers OBJECTIVE_ID QUESTION_ID)) => {:status ::http-api/success 
                                                                         :result [the-stored-answer]}))
