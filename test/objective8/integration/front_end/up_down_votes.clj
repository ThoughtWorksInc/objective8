(ns objective8.integration.front-end.up-down-votes
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.config :as config]
            [objective8.api.http :as http-api]
            [objective8.integration.integration-helpers :as helpers]))

(binding [config/enable-csrf false]
  (fact "Can up vote answers"
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
                     :params {:vote-on-uri "/uri/for/answer" :refer "/objectives/1/questions/1"}) =>
          (contains {:response (contains {:status 302
                                                     :headers (contains {"Location" "/objectives/1/questions/1"})})})
          (provided
           (http-api/create-up-down-vote {:vote-on-uri "/uri/for/answer" :created-by-id 100 :vote-type "up"}) => {:status ::http-api/success})))

  (fact "Can down vote answers"
        (against-background
         (oauth/access-token anything anything anything) => {:user_id 100}
         (http-api/create-user anything) => {:status ::http-api/success
                                             :result {:_id 100}}
         (http-api/get-question 1 1) => {:is :a-question})

        (let [signed-in-session (-> (helpers/test-context)
                                    (helpers/with-sign-in "http://localhost:8080/objectives/1/questions/1"))]
          (:response signed-in-session) => (contains {:status 200})
          (p/request signed-in-session
                     "http://localhost:8080/meta/down-vote"
                     :request-method :post
                     :params {:vote-on-uri "/uri/for/answer" :refer "/objectives/1/questions/1"})) =>
          (contains {:response (contains {:status 302
                                          :headers (contains {"Location" "/objectives/1/questions/1"})})})
          (provided
           (http-api/create-up-down-vote {:vote-on-uri "/uri/for/answer" :created-by-id 100 :vote-type "down"}) => {:status ::http-api/success}))) 
