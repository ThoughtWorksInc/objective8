(ns objective8.integration.front-end.objectives
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.handlers.front-end :as front-end]
            [objective8.http-api :as http-api]
            [objective8.config :as config]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.core :as core]))

(def TWITTER_ID "TWITTER_ID")

(def OBJECTIVE_ID 234)
(def USER_ID 1)

(def objectives-create-request (mock/request :get "/objectives/create"))
(def objectives-post-request (mock/request :post "/objectives"))
(def objective-view-get-request (mock/request :get (str "/objectives/" OBJECTIVE_ID)))
(def invalid-objective-view-get-request (mock/request :get (str "/objectives/" "not-an-objective-id")))

(def default-app (core/app core/app-config))

(def user-session (helpers/test-context))

(def basic-objective {:title "my objective title"
                      :goal-1 "my objective goal"
                      :description "my objective description"
                      :end-date (utils/string->date-time "2012-12-12")
                      :uri (str "/objectives/" OBJECTIVE_ID)})



(facts "objectives"
       (against-background
        ;; Twitter authentication background
        (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
        (http-api/create-user anything) => {:status ::http-api/success
                                            :result {:_id USER_ID}}
        (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                        :result {:_id USER_ID
                                                                 :username "username"}})
       (binding [config/enable-csrf false]
         (fact "authorised user can post and retrieve objective"
               (against-background (http-api/create-objective 
                                     (contains {:title "my objective title"
                                                :goal-1 "my objective goal"
                                                :goal-2 "second goal"
                                                :goal-3 "third goal"
                                                :description "my objective description"
                                                :end-date anything
                                                :created-by-id USER_ID})) => {:status ::http-api/success
                                                                              :result {:_id OBJECTIVE_ID}})
               (let [params {:title "my objective title"
                             :goal-1 "my objective goal"
                             :goal-2 "second goal"
                             :goal-3 "third goal"
                             :description "my objective description"}
                     response (:response
                                (-> user-session
                                    (helpers/with-sign-in "http://localhost:8080/objectives/create")
                                    (p/request "http://localhost:8080/objectives"
                                               :request-method :post
                                               :params params)))]
                 (:flash response) => (contains {:type :flash-message :message :objective-view/created-message})
                 (-> response
                     :headers
                     (get "Location")) => (contains (str "/objectives/" OBJECTIVE_ID)))))

       (fact "Any user can view an objective"
             (against-background
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result basic-objective}
               (http-api/get-comments anything)=> {:status ::http-api/success :result []}
               (http-api/retrieve-candidates OBJECTIVE_ID) => {:status ::http-api/success :result []}
               (http-api/retrieve-questions OBJECTIVE_ID) => {:status ::http-api/success :result []})
             (default-app objective-view-get-request) => (contains {:status 200})
             (default-app objective-view-get-request) => (contains {:body (contains "my objective title")})
             (default-app objective-view-get-request) => (contains {:body (contains "my objective description")}))

       (fact "When a signed in user views an objective, the objective contains user specific information"
             (against-background
               (http-api/get-comments anything)=> {:status ::http-api/success :result []}
               (http-api/retrieve-candidates OBJECTIVE_ID) => {:status ::http-api/success :result []}
               (http-api/retrieve-questions OBJECTIVE_ID) => {:status ::http-api/success :result []})
             (-> user-session
                 helpers/sign-in-as-existing-user
                 (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID))) => anything
             (provided
              (http-api/get-objective OBJECTIVE_ID {:signed-in-id USER_ID}) => {:status ::http-api/success
                                                                                :result basic-objective}))

       (fact "A user should receive a 404 if an objective doesn't exist"
             (against-background
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/not-found})
             (default-app objective-view-get-request) => (contains {:status 404}))

       (facts "about comments"
              (against-background
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result basic-objective}
               (http-api/retrieve-candidates OBJECTIVE_ID) => {:status ::http-api/success :result []}
               (http-api/retrieve-questions OBJECTIVE_ID) => {:status ::http-api/success :result []}
               (http-api/get-comments anything) => {:status ::http-api/success :result []})

              (fact "Any user can view comments with votes on an objective"
                    (against-background
                      (http-api/get-comments anything) => {:status ::http-api/success
                                                           :result [{:_id 1
                                                                     :_created_at "2015-02-12T16:46:18.838Z"
                                                                     :objective-id OBJECTIVE_ID
                                                                     :created-by-id USER_ID
                                                                     :comment "Comment 1"
                                                                     :uri "/comments/1"
                                                                     :votes {:up 123456789 :down 987654321}}]})
                    (let [{response :response} (p/request user-session (str "http://localhost:8080/objectives/" OBJECTIVE_ID))]
                      (:body response) => (contains "Comment 1")
                      ;;(:body response) => (contains "123456789")
                      ;;(:body response) => (contains "987654321")
                      ))

              (fact "An objective that is in drafting cannot be commented on"
                    (against-background
                     (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                               :result (assoc basic-objective :status "drafting")})
                    (let [{response :response} (p/request user-session (str "http://localhost:8080/objectives/" OBJECTIVE_ID))]
                      (:body response) =not=> (contains "clj-comment-create"))))

       (fact "A user should see an error page when they attempt to access an objective with a non-integer ID"
             (default-app invalid-objective-view-get-request) => (contains {:status 404})))


(facts "About viewing the list of objectives"
       (against-background
        ;; Twitter authentication background
        (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
        (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                        :result {:_id USER_ID
                                                                 :username "username"}})
       
       (fact "when the viewing user is not signed in, the get-objectives query is made with no user information"
             (p/request user-session "http://localhost:8080/objectives") => anything
             (provided 
               (http-api/get-objectives) => {:status ::http-api/success
                                             :result []}))

       (fact "when the viewing user is signed in, the get-objectives query is made for that user"
             (-> user-session
                 helpers/sign-in-as-existing-user
                 (p/request "http://localhost:8080/objectives")) => anything
             (provided
               (http-api/get-objectives {:signed-in-id USER_ID}) => {:status ::http-api/success
                                                                     :result []})))
