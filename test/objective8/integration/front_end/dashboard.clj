(ns objective8.integration.front-end.dashboard
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.http-api :as http-api]
            [objective8.utils :as utils]
            [objective8.config :as config]
            [objective8.core :as core]
            [objective8.integration.integration-helpers :as ih]))

(def user-session (ih/test-context))

(def OBJECTIVE_ID 3)
(def OBJECTIVE_URI (str "/objectives/" OBJECTIVE_ID))
(def STARS_COUNT 23432)
(def ANSWERS_COUNT 34343)
(def TWITTER_ID 2)
(def USER_ID 4)
(def QUESTION_ID 5)
(def NOTE_ID 42)
(def QUESTION_URI (str "/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))
(def NO_ANSWER_MESSAGE "No answers were provided for this question.")
(def NO_QUESTION_MESSAGE "No questions have been asked for this objective")

(def writer-for-objective {:_id USER_ID :username "username" :writer-records [{:objective-id OBJECTIVE_ID}]})

(background
 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
 (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                 :result writer-for-objective}
 (http-api/get-user anything) => {:result writer-for-objective})

(facts "about the questions dashboard for writers"
       (against-background
        (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                  :result {:entity :objective
                                                           :title "Objective title"
                                                           :_id OBJECTIVE_ID
                                                           :meta {:stars-count STARS_COUNT}}}

        (http-api/retrieve-questions OBJECTIVE_ID {:sorted-by "answers"})
        => {:status ::http-api/success
            :result [{:entity :question
                      :uri QUESTION_URI
                      :question "test question"
                      :answer-count ANSWERS_COUNT}]}

        (http-api/retrieve-answers QUESTION_URI {:sorted-by "up-votes"}) => {:status ::http-api/success
                                                                             :result [{:entity :answer
                                                                                       :answer "test answer"}]})
       (fact "can see answers for specific questions"
             (let [{response :response} (-> user-session
                                            ih/sign-in-as-existing-user
                                            (p/request (utils/path-for :fe/dashboard-questions :id OBJECTIVE_ID)))]
               (:status response) => 200
               (:body response) => (contains "Objective title")
               (:body response) => (contains "test question")
               (:body response) => (contains "test answer")))

       (fact "see message noting there is no answer when question has no answers"
             (against-background
              (http-api/retrieve-answers QUESTION_URI {:sorted-by "up-votes"}) => {:status ::http-api/success
                                                                                   :result []})
             (let [{response :response} (-> user-session
                                            ih/sign-in-as-existing-user
                                            (p/request (utils/path-for :fe/dashboard-questions :id OBJECTIVE_ID)))]
               (:status response) => 200
               (:body response) => (contains "Objective title")
               (:body response) => (contains "test question")
               (:body response) => (contains NO_ANSWER_MESSAGE)))

       (fact "see message noting there are no questions when no questions were submitted to objective"
             (against-background
              (http-api/retrieve-questions OBJECTIVE_ID {:sorted-by "answers"}) => {:status ::http-api/success
                                                                                    :result []})
             (let [{response :response} (-> user-session
                                            ih/sign-in-as-existing-user
                                            (p/request (utils/path-for :fe/dashboard-questions :id OBJECTIVE_ID)))]
               (:status response) => 200
               (:body response) => (contains "Objective title")
               (:body response) => (contains NO_QUESTION_MESSAGE)))
       
       (fact "get objectives with stars-count"
             (let [{response :response} (-> user-session
                                            ih/sign-in-as-existing-user
                                            (p/request (utils/path-for :fe/dashboard-questions :id OBJECTIVE_ID)))]
               (:status response) => 200
               (:body response) => (contains (str STARS_COUNT))))
       
       (fact "get questions with answer-count"
             (let [{response :response} (-> user-session
                                            ih/sign-in-as-existing-user
                                            (p/request (utils/path-for :fe/dashboard-questions :id OBJECTIVE_ID)))]
               (:status response) => 200
               (:body response) => (contains (str "(" ANSWERS_COUNT ")"))))

       (fact "can get answers sorted by down votes"
             (-> user-session
                 ih/sign-in-as-existing-user
                 (p/request (str (utils/path-for :fe/dashboard-questions :id OBJECTIVE_ID) "?sorted-by=down-votes"))
                 :response
                 :status) => 200
                 (provided
                  (http-api/retrieve-answers QUESTION_URI {:sorted-by "down-votes"}) => {:status ::http-api/success
                                                                                         :result [{:entity :answer
                                                                                                   :answer "test answer"}]}))

       (fact "can see form if answer has no note"
             (let [{response :response} (-> user-session
                                            ih/sign-in-as-existing-user
                                            (p/request (utils/path-for :fe/dashboard-questions :id OBJECTIVE_ID)))]
               (:status response) => 200
               (:body response) => (contains "note-on-uri")))


       (fact "can see note text if the answer has a note"

             (against-background
               (http-api/retrieve-answers QUESTION_URI {:sorted-by "up-votes"}) => {:status ::http-api/success
                                                                                    :result [{:entity :answer
                                                                                              :answer "test answer with a note"
                                                                                              :note "test note"
                                                                                              }]}) 
             (let [{response :response} (-> user-session
                                            ih/sign-in-as-existing-user
                                            (p/request (utils/path-for :fe/dashboard-questions :id OBJECTIVE_ID)))]
               (:status response) => 200
               (:body response) => (contains "test note"))))

(facts "notes"
       (binding [config/enable-csrf false]
         (fact "authorised writer can post note against question"
               (let [params {:refer (str "/objectives/" OBJECTIVE_ID "/dashboard/questions")
                             :note "Test note"
                             :note-on-uri QUESTION_URI}
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/post-writer-note) 
                                                         :request-method :post 
                                                         :params params))]
                 {:headers (:headers response)
                  :status (:status response)}) => (just {:headers (ih/location-contains (str "/objectives/" OBJECTIVE_ID "/dashboard/questions"))
                                                         :status 302}) 

               (provided
                 (http-api/post-writer-note {:note "Test note"
                                             :note-on-uri QUESTION_URI
                                             :created-by-id USER_ID}) => {:status ::http-api/success
                                                    :result []}))

         (fact "authorised writer should recieve a 403 response when http-api returns forbidden"
               (let [params {:refer (str "/objectives/" OBJECTIVE_ID "/dashboard/questions")
                             :note "Test note"
                             :note-on-uri QUESTION_URI}
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/post-writer-note) 
                                                         :request-method :post 
                                                         :params params))]
                 {:status (:status response)}) => (just { :status 403}) 

               (provided
                 (http-api/post-writer-note {:note "Test note"
                                             :note-on-uri QUESTION_URI
                                             :created-by-id USER_ID
                                             }) => {:status ::http-api/forbidden
                                                    :result []}))

         (fact "authorised writer should recieve a 404 response when http-api returns entity-not-found"
               (let [params {:refer (str "/objectives/" OBJECTIVE_ID "/dashboard/questions")
                             :note "Test note"
                             :note-on-uri QUESTION_URI}
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/post-writer-note) 
                                                         :request-method :post 
                                                         :params params))]
                 {:status (:status response)}) => (just { :status 404}) 

               (provided
                 (http-api/post-writer-note {:note "Test note"
                                             :note-on-uri QUESTION_URI
                                             :created-by-id USER_ID
                                             }) => {:status ::http-api/entity-not-found
                                                    :result []}))

         (fact "unauthorised user should be redirected to sign in page when attempting to post note against question"
               (let [params {:refer (str "/objectives/" OBJECTIVE_ID "/dashboard/questions")
                             :note "Test note"
                             :note-on-uri QUESTION_URI}
                     {response :response} (-> user-session
                                              (p/request (utils/path-for :fe/post-writer-note) 
                                                         :request-method :post 
                                                         :params params))]
                 {:headers (:headers response)
                  :status (:status response)}) => (just {:headers (ih/location-contains "sign-in") :status 302}))))

(facts "about the comments dashboard for writers"
       (against-background
         (http-api/get-objective anything) => {:status ::http-api/success
                                               :result {:entity :objective
                                                        :title "Objective title"
                                                        :_id OBJECTIVE_ID
                                                        :meta {:stars-count STARS_COUNT
                                                               :comments-count 1}}}

         (http-api/get-all-drafts anything) => {:status ::http-api/success
                                                :result [{:meta {:comments-count 1}
                                                          :_created_at "2015-04-04T12:00:00.000Z"}]}

         (http-api/get-comments anything) => {:status ::http-api/success
                                              :result [{:comment "A comment"
                                                        :_created_at "2015-01-01T01:01:00.000Z"
                                                        :username "A User"
                                                        :votes {:up 5 :down 3}}]})

       (fact "can get comments sorted by number of up votes"
             (-> user-session
                 ih/sign-in-as-existing-user
                 (p/request (str (utils/path-for :fe/dashboard-comments :id OBJECTIVE_ID) "?sorted-by=up-votes"))
                 :response
                 :status) => 200
             (provided
               (http-api/get-comments anything {:sorted-by "up-votes"}) => {:status ::http-api/success
                                                                            :result []})))
