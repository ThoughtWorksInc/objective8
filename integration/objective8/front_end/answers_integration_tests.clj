(ns objective8.front-end.answers-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.http-api :as http-api]
            [objective8.config :as config]
            [objective8.integration-helpers :as helpers]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)
(def QUESTION_ID 2)

(facts "answers" :integration
       (binding [config/enable-csrf false]
         (fact "authorised user can post and retrieve answer to a question"
              (against-background
                  (http-api/get-question OBJECTIVE_ID QUESTION_ID) => {:status 200})
              (against-background
                  (http-api/create-answer {:answer "The answer"
                                           :objective-id OBJECTIVE_ID
                                           :question-id QUESTION_ID
                                           :created-by-id USER_ID}) => {:status ::http-api/success
                                                                        :result  {:_id 12
                                                                                  :objective-id OBJECTIVE_ID
                                                                                  :question-id QUESTION_ID
                                                                                  :created-by-id USER_ID
                                                                                  :answer "The answer"}})
              (against-background
                  (oauth/access-token anything anything anything) => {:user_id USER_ID}
                  (http-api/create-user anything) => {:_id USER_ID})
               (let [user-session (helpers/test-context)
                     params {:answer "The answer"}
                     peridot-response (-> user-session
                                          (helpers/with-sign-in (str "http://localhost:8080/objectives/" OBJECTIVE_ID 
                                                                     "/questions/" QUESTION_ID))
                                          (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID 
                                                          "/questions/" QUESTION_ID "/answers")
                                                     :request-method :post
                                                     :params params))]
                 peridot-response => (helpers/flash-message-contains "Your answer has been added!")
                 peridot-response => (helpers/headers-location (str "/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))))))
