(ns objective8.integration.front-end.up-down-votes
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.config :as config]
            [objective8.http-api :as http-api]
            [objective8.integration.integration-helpers :as helpers]))

(binding [config/enable-csrf false]
  (future-fact "Can up vote answers
               TODO - Use URIs instead of global ids? e.g. objective/1/questions/7 will always be unique"
        (against-background
          (oauth/access-token anything anything anything) => {:user_id 100}
          (http-api/create-user anything) => {:status ::http-api/success
                                              :result {:_id 100}}
          (http-api/get-question 1 1) => {:is :a-question})
        (let [signed-in-session (-> (helpers/test-context)
                                    (helpers/with-sign-in "http://localhost:8080/objectives/1/questions/1"))]
          (:response signed-in-session) => (contains {:status 200})
          (p/request signed-in-session
                     "http://localhost:8080/meta/up-vote"
                     :request-method :post
                     :params {:uri "/objectives/1/questions/1/answers/1"}) => (contains {:response (contains {:status 302
                                                                                         :headers (contains {"Location" (contains "/objectives/1/questions/1")})})})
         (provided
           (http-api/create-up-down-vote {:global-id 1 :created-by-id 100 :vote-type "up"}) => :vote)))) 
