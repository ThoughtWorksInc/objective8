(ns objective8.front-end.objectives-integration-tests
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.handlers.front-end :as front-end]
            [objective8.http-api :as http-api]
            [objective8.config :as config]
            [objective8.integration-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.core :as core]))
(def TWITTER_ID "TWITTER_ID")

(def OBJECTIVE_ID 234)
(def USER_ID 1)

(def objectives-create-request (mock/request :get "/objectives/create"))
(def objectives-post-request (mock/request :post "/objectives"))
(def objective-view-get-request (mock/request :get (str "/objectives/" OBJECTIVE_ID)))

(def default-app (core/app core/app-config))


(binding [config/enable-csrf false]
  (fact "authorised user can post and retrieve objective"
        (against-background (http-api/create-objective
                              {:title "my objective title"
                               :goals "my objective goals"
                               :description "my objective description"
                               :end-date (utils/string->date-time "2012-12-12")
                               :created-by-id USER_ID}) => {:_id OBJECTIVE_ID})
        (against-background
          ;; Twitter authentication background
         (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
         (http-api/create-user anything) => {:_id USER_ID})
        (let [user-session (helpers/test-context)
              params {:title "my objective title"
                      :goals "my objective goals"
                      :description "my objective description"
                      :end-date "2012-12-12"}
              response (:response
                         (-> user-session
                             (helpers/with-sign-in "http://localhost:8080/objectives/create")
                             (p/request "http://localhost:8080/objectives"
                                        :request-method :post
                                        :params params)))]
          (:flash response) => (contains "Your objective has been created!")
          (-> response
              :headers
              (get "Location")) => (contains (str "/objectives/" OBJECTIVE_ID)))))

(fact "Any user can view an objective"
      (against-background
        (http-api/get-objective OBJECTIVE_ID) => {:title "my objective title"
                                                  :goals "my objective goals"
                                                  :description "my objective description"
                                                  :end-date (utils/string->date-time "2015-12-01")})
      (default-app objective-view-get-request) => (contains {:status 200})
      (default-app objective-view-get-request) => (contains {:body (contains "my objective title")})
      (default-app objective-view-get-request) => (contains {:body (contains "my objective goals")})
      (default-app objective-view-get-request) => (contains {:body (contains "my objective description")})
      (default-app objective-view-get-request) => (contains {:body (contains "01-12-2015")}))
