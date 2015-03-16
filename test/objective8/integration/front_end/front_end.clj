(ns objective8.integration.front-end.front-end
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.front-end-helpers :refer [request->comment request->objective]]
            [objective8.storage.storage :as storage]
            [objective8.handlers.front-end :as front-end]
            [objective8.http-api :as http-api]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.config :as config]
            [objective8.core :as core]))

(def USER_ID 1)

(def OBJECTIVE_ID 234)

(def objectives-create-request (mock/request :get "/objectives/create"))
(def objectives-post-request (mock/request :post "/objectives"))

(def default-app (core/app core/app-config))

(defn check-status [status]
  (fn [peridot-response]
    (= status (get-in peridot-response [:response :status]))))

(defn check-redirect-url [url-fragment]
  (fn [peridot-response]
    ((contains url-fragment) (get-in peridot-response [:response :headers "Location"]))))

(facts "front end" :integration
       (binding [config/enable-csrf false]
         (facts "authorisation"
                (facts "signed in users"
                       (against-background
                         ;; Twitter authentication background
                         (oauth/access-token anything anything anything) => {:user_id USER_ID}
                         (http-api/create-user anything) => {:status ::http-api/success
                                                             :result {:_id USER_ID}})
                       (fact "can reach the create objective page"
                             (let [result (-> (p/session default-app)
                                              (helpers/with-sign-in "http://localhost:8080/objectives/create"))]
                               result => (check-status 200)
                               (:request result) => (contains {:uri "/objectives/create"})))
                       (fact "can post a new objective"
                             (against-background
                               (request->objective anything anything) => :an-objective
                               (http-api/create-objective :an-objective) => {:status ::http-api/success
                                                                             :result {:_id OBJECTIVE_ID}})
                             (let [response
                                   (-> (p/session default-app)
                                       (helpers/with-sign-in "http://localhost:8080/objectives/create")
                                       (p/request "http://localhost:8080/objectives" :request-method :post))]
                               response => (check-status 302)
                               response => (check-redirect-url (str "/objectives/" OBJECTIVE_ID))))
                       ;TODO APi returns 302, and unauthorised returns 302, do we need these tests?
                       (fact "can post a new comment to an objective"
                             (against-background
                               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success} 
                               (request->comment anything anything) => :a-comment
                               (http-api/create-comment :a-comment) => {:status ::http-api/success
                                                                        :result {:_id "the-comment-id"}})
                             (let [response
                                   (-> (p/session default-app)
                                       (helpers/with-sign-in (str utils/host-url "/objectives/" OBJECTIVE_ID))
                                       (p/request "http://localhost:8080/comments" :request-method :post
                                                  :params {:objective-id OBJECTIVE_ID}))]
                               response => (check-status 302))))
                (facts "unauthorised users"
                       (fact "cannot reach the objective creation page"
                             (default-app objectives-create-request) => (contains {:status 302}))
                       (fact "cannot post a new objective"
                             (default-app objectives-post-request) => (contains {:status 302}))
                       (fact "cannot post a comment"
                             (default-app (mock/request :post "/comments")) => (contains {:status 302}))))))
