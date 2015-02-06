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

(def the-user-id "user_id")

(binding [config/enable-csrf false]
  (fact "authorised user can post and retrieve comment against an objective"
      (against-background (http-api/create-comment {:comment "The comment"
                                                    :objective-id "OBJECTIVE_GUID"
                                                    :user-id (str "twitter-" the-user-id)}) => {:_id "some-id"})
      (against-background
        ;; Twitter authentication background
        (oauth/access-token anything anything anything) => {:user_id the-user-id})
      (let [store (atom {})
              app-config (into core/app-config {:store store})
              user-session (p/session (core/app app-config))
              params {:comment "The comment"
                      :objective-id "OBJECTIVE_GUID"}
              response (:response
                         (-> user-session
                             (helpers/with-sign-in "http://localhost:8080/objectives/OBJECTIVE_GUID")
                             (p/request "http://localhost:8080/comments"
                                        :request-method :post
                                        :params params)))]
          (:flash response) => (contains "Your comment has been added!")
          (-> response
              :headers
              (get "Location")) => (contains "/objectives/OBJECTIVE_GUID"))))
