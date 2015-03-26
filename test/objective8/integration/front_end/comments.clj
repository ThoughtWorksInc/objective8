(ns objective8.integration.front-end.comments
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.handlers.front-end :as front-end]
            [objective8.http-api :as http-api]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.core :as core]))

(def USER_ID 1)
(def COMMENT_ID 123)
(def OBJECTIVE_ID 234)
(def GLOBAL_ID 223)
(def OBJECTIVE_URI (str "/objectives/" OBJECTIVE_ID))

(facts "comments"
       (binding [config/enable-csrf false]
         (fact "authorised user can post and retrieve comment against an objective"
              (against-background
                  (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success})
              (against-background
               (http-api/post-comment {:comment "The comment"
                                       :comment-on-uri OBJECTIVE_URI
                                       :created-by-id USER_ID}) => {:status ::http-api/success
                                                                    :result  {:_id 12
                                                                              :created-by-id USER_ID
                                                                              :comment-on-uri OBJECTIVE_URI
                                                                              :comment "The comment"}})
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id USER_ID}
                 (http-api/create-user anything) => {:status ::http-api/success
                                                     :result {:_id USER_ID}})
               (let [user-session (helpers/test-context)
                     params {:comment "The comment"
                             :refer (str "/objectives/" OBJECTIVE_ID)
                             :comment-on-uri OBJECTIVE_URI}
                     {response :response} (-> user-session
                                              (helpers/with-sign-in (str "http://localhost:8080/objectives/" OBJECTIVE_ID))
                                              (p/request (utils/path-for :fe/post-comment)
                                                         :request-method :post
                                                         :params params))]
                 (:flash response) => (contains "Your comment has been added!")
                 (:headers response) => (helpers/location-contains (str "/objectives/" OBJECTIVE_ID))
                 (:status response) => 302))))
