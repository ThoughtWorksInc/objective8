(ns objective8.integration.front-end.answers
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.utils :as utils]
            [objective8.http-api :as http-api]
            [objective8.config :as config]
            [objective8.integration.integration-helpers :as helpers]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)
(def QUESTION_ID 2)

(def TWITTER_ID "twitter-123456")

(def user-session (helpers/test-context))

(facts "about creating answers"
       (binding [config/enable-csrf false]
         (tabular
          (fact "validation errors are reported"
                (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                                 :result {:_id USER_ID :username "username" :writer-records []}}
                 (http-api/get-user anything) => {:result {:_id USER_ID :username "username" :writer-records []}}
                 (http-api/get-question OBJECTIVE_ID QUESTION_ID) => {:status ::http-api/success}
                 (http-api/retrieve-answers anything) => {:status ::http-api/success :result []}
                 (http-api/get-objective anything) => {:status ::http-api/success :result {:_id OBJECTIVE_ID :status "open"}})
                
                (-> user-session
                    helpers/sign-in-as-existing-user
                    (p/request (utils/path-for :fe/add-answer-form-post :id OBJECTIVE_ID :q-id QUESTION_ID)
                               :request-method :post
                               :params {:answer ?answer})
                    p/follow-redirect
                    :response
                    :body) => (contains ?expected-error-message))
          
          ?answer                          ?expected-error-message
          ""                               "clj-answer-empty-error"
          (helpers/string-of-length 501)   "clj-answer-length-error")))

(facts "answers"
       (binding [config/enable-csrf false]
         (fact "authorised user can post and retrieve answer to a question"
               (against-background
                (http-api/get-question OBJECTIVE_ID QUESTION_ID) => {:status ::http-api/success})
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
                  (http-api/create-user anything) => {:status ::http-api/success
                                                      :result {:_id USER_ID}})
              (let [params {:answer "The answer"}
                    peridot-response (-> user-session
                                         (helpers/with-sign-in (str "http://localhost:8080/objectives/" OBJECTIVE_ID 
                                                                    "/questions/" QUESTION_ID))
                                         (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID 
                                                         "/questions/" QUESTION_ID "/answers")
                                                    :request-method :post
                                                     :params params))]
                peridot-response => (helpers/flash-message-contains {:type :flash-message
                                                                     :message :question-view/added-answer-message})
                peridot-response => (helpers/headers-location (str "/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))))))
