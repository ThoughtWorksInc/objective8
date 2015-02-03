(ns d-cent.front-end.objectives-integration-tests
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [d-cent.handlers.front-end :as front-end]
            [d-cent.http-api :as http-api]
            [d-cent.integration-helpers :as helpers]
            [d-cent.utils :as utils]
            [d-cent.core :as core]))
(def the-user-id "user_id")

(def objectives-create-request (mock/request :get "/objectives/create"))
(def objectives-post-request (mock/request :post "/objectives"))
(def objective-view-get-request (mock/request :get "/objectives/OBJECTIVE_GUID"))

(def default-app (core/app core/app-config))

(fact "authorised user can post and retrieve objective"
      (against-background (http-api/create-objective
                           {:title "my objective title"
                            :goals "my objective goals"
                            :description "my objective description"
                            :end-date (utils/string->time-stamp "2012-12-12")
                            :created-by "twitter-user_id"}) => {:_id "some-id"})
      (against-background
       ;; Twitter authentication background
       (oauth/access-token anything anything anything) => {:user_id the-user-id})
      (let [store (atom {})
            app-config (into core/app-config {:store store})
            user-session (p/session (core/app app-config))
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
            (get "Location")) => (contains "/objectives/some-id")))

(fact "Any user can view an objective"
      (against-background
       (http-api/get-objective "OBJECTIVE_GUID") => {:title "my objective title"
                                                     :goals "my objective goals"
                                                     :description "my objective description"
                                                     :end-date "2015-01-30T00:00:00.000Z"})
      (default-app objective-view-get-request) => (contains {:status 200})
      (default-app objective-view-get-request) => (contains {:body (contains "my objective title")})
      (default-app objective-view-get-request) => (contains {:body (contains "my objective goals")})
      (default-app objective-view-get-request) => (contains {:body (contains "my objective description")}))
