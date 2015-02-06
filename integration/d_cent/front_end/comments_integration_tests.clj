(ns d-cent.front-end.comments-integration-tests
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [d-cent.handlers.front-end :as front-end]
            [d-cent.http-api :as http-api]
            [d-cent.config :as config]
            [d-cent.integration-helpers :as helpers]
            [d-cent.core :as core]))

(def USER_ID 1)
(def COMMENT_ID 123)
(def OBJECTIVE_ID 234)

(binding [config/enable-csrf false]
  (fact "authorised user can post and retrieve comment against an objective"
      (against-background (http-api/create-comment {:comment "The comment"
                                                    :objective-id OBJECTIVE_ID
                                                    :user-id USER_ID}) => {:_id COMMENT_ID})
      (against-background
       (oauth/access-token anything anything anything) => {:user_id USER_ID}
       (http-api/create-user anything) => {:_id USER_ID})
      (let [store (atom {})
              app-config (into core/app-config {:store store})
              user-session (p/session (core/app app-config))
              params {:comment "The comment"
                      :objective-id OBJECTIVE_ID}
              response (:response
                         (-> user-session
                             (helpers/with-sign-in (str "http://localhost:8080/objectives/" OBJECTIVE_ID))
                             (p/request "http://localhost:8080/comments"
                                        :request-method :post
                                        :params params)))]
          (:flash response) => (contains "Your comment has been added!")
          (-> response
              :headers
              (get "Location")) => (contains (str "/objectives/" OBJECTIVE_ID)))))
