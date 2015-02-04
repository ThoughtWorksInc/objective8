(ns d-cent.front-end.front-end-integration-tests
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [d-cent.objectives :refer [request->objective]]
            [d-cent.storage :as storage]
            [d-cent.handlers.front-end :as front-end]
            [d-cent.http-api :as http-api]
            [d-cent.integration-helpers :as helpers]
            [d-cent.utils :as utils]
            [d-cent.core :as core]))

(def the-user-id "user_id")

(def objectives-create-request (mock/request :get "/objectives/create"))
(def objectives-post-request (mock/request :post "/objectives"))

(def default-app (core/app core/app-config))

(defn check-status [status]
  (fn [peridot-response]
    (= status (get-in peridot-response [:response :status]))))

(defn check-redirect-url [url-fragment]
  (fn [peridot-response]
    ((contains url-fragment) (get-in peridot-response [:response :headers "Location"]))))

(facts "authorisation"
       (facts "signed in users"
              (against-background
               ;; Twitter authentication background
               (oauth/access-token anything anything anything) => {:user_id the-user-id})
              (fact "can reach the create objective page"
                    (let [result (-> (p/session default-app)
                                     (helpers/with-sign-in "http://localhost:8080/objectives/create"))]
                      result => (check-status 200)
                      (:request result) => (contains {:uri "/objectives/create"})))
              (fact "can post a new objective"
                    (against-background
                     (request->objective anything) => :an-objective
                     (http-api/create-objective :an-objective) => {:_id "the-objective-id"})
                    (let [response
                      (-> (p/session default-app)
                          (helpers/with-sign-in "http://localhost:8080/objectives/create")
                          (p/request "http://localhost:8080/objectives" :request-method :post))]
                      response => (check-status 302)
                      response => (check-redirect-url "/objectives/the-objective-id"))))
       (facts "unauthorised users"
              (fact "cannot reach the objective creation page"
                    (default-app objectives-create-request)
                    => (contains {:status 302}))
              (fact "cannot post a new objective"
                    (default-app objectives-post-request)
                    => (contains {:status 302}))))
