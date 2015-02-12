(ns objective8.front-end.questions-integration-tests
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.handlers.front-end :as front-end]
            [objective8.http-api :as http-api]
            [objective8.config :as config]
            [objective8.integration-helpers :as helpers]
            [objective8.core :as core]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)
(def QUESTION_ID 42)


(facts "questions" :integration
       (binding [config/enable-csrf false]
         (fact "authorised user can post and retrieve question against an objective"
              (against-background
                  (http-api/get-objective OBJECTIVE_ID) => {:status 200})
              (against-background
                  (http-api/create-question {:question "The meaning of life?"
                                            :created-by-id USER_ID}) => {:_id QUESTION_ID
                                                                         :objective-id OBJECTIVE_ID})
              (against-background
                  (oauth/access-token anything anything anything) => {:user_id USER_ID}
                  (http-api/create-user anything) => {:_id USER_ID})
               (let [user-session (helpers/test-context)
                     params {:question "The meaning of life?"}
                     peridot-response (-> user-session
                                          (helpers/with-sign-in (str "http://localhost:8080/objectives/" OBJECTIVE_ID "/questions/add"))
                                          (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID "/questions")
                                                     :request-method :post
                                                     :params params))]
                 peridot-response => (helpers/flash-message-contains "Your question has been added!")
                 peridot-response => (helpers/headers-location (str "/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))))))
