(ns objective8.integration.front-end.dashboard
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.http-api :as http-api]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration.integration-helpers :as ih]))

(def user-session (ih/test-context))

(def OBJECTIVE_ID 3)
(def TWITTER_ID 2)
(def USER_ID 4)
(def QUESTION_ID 5)
(def QUESTION_URI (str "/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))
(def NO_ANSWER_MESSAGE "No answers were provided for this question.")
(def NO_QUESTION_MESSAGE "No questions were asked for this objective.")

(def writer-for-objective {:_id USER_ID :writer-records [{:objective-id OBJECTIVE_ID}]})

(background
 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
 (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                 :result writer-for-objective}
 (http-api/get-user anything) => {:result writer-for-objective})

(facts "about the questions dashboard for writers"
       (against-background
        (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
        (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                        :result writer-for-objective}
        (http-api/get-user anything) => {:result writer-for-objective})

       (against-background
         (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                   :result {:entity :objective
                                                            :title "Objective title"
                                                            :_id OBJECTIVE_ID}}
         (http-api/retrieve-questions OBJECTIVE_ID) => {:status ::http-api/success
                                                        :result [{:entity :question
                                                                  :uri QUESTION_URI
                                                                  :question "test question"}]}
         (http-api/retrieve-answers QUESTION_URI) => {:status ::http-api/success
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
               (http-api/retrieve-answers QUESTION_URI) => {:status ::http-api/success
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
               (http-api/retrieve-questions OBJECTIVE_ID) => {:status ::http-api/success
                                                              :result []})
             (let [{response :response} (-> user-session
                                            ih/sign-in-as-existing-user
                                            (p/request (utils/path-for :fe/dashboard-questions :id OBJECTIVE_ID)))]
               (:status response) => 200
               (:body response) => (contains "Objective title")
               (:body response) => (contains NO_QUESTION_MESSAGE))))

