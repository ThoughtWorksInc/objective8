(ns d-cent.front-end-integration-tests
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [d-cent.objectives :refer [request->objective find-by-id]]
            [d-cent.storage :as storage]
            [d-cent.handlers.front-end :as front-end]
            [d-cent.http-api :as api]
            [d-cent.core :as core]
            [d-cent.integration-helpers :as helpers]))

(def the-user-id "user_id")

(def objectives-create-request (mock/request :get "/objectives/create"))
(def objectives-post-request (mock/request :post "/objectives"))
(def objective-view-get-request (mock/request :get "/objectives/some-long-id"))

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
                     (api/create-objective :an-objective) => {:_id "the-objective-id"})
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
                    => (contains {:status 302}))
              (fact "can access objective view"
                    (default-app objective-view-get-request)
                    => (contains {:status 200})
                    (provided
                     (find-by-id anything "some-long-id") => :an-objective))))

(fact "authorised user can post and retrieve objective"
      (against-background (api/create-objective 
                           {:title "my objective title"
                            :goals "my objective goals"
                            :description "my objective description"
                            :end-date "my objective end-date"
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
                    :end-date "my objective end-date"}
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
