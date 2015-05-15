(ns objective8.integration.front-end.questions
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.handlers.front-end :as front-end]
            [objective8.api.http :as http-api]
            [objective8.config :as config]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.core :as core]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)
(def QUESTION_ID 42)
(def INVALID_ID "not-an-int-id")
(def QUESTION_URI (str "/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))
(def question-view-get-request (mock/request :get (str utils/host-url "/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID)))
(defn invalid-question-get-request [objective-id question-id]
  (mock/request :get (str utils/host-url "/objectives/" objective-id "/questions/" question-id)))
(def questions-view-get-request (mock/request :get (str utils/host-url "/objectives/" OBJECTIVE_ID "/questions")))

(def default-app (core/app helpers/test-config))

(def user-session (helpers/test-context))

(facts "about questions"
       (binding [config/enable-csrf false]
         (fact "authorised user can post and retrieve a question against an objective"
               (against-background
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success}
                 (http-api/create-question {:objective-id OBJECTIVE_ID
                                            :created-by-id USER_ID
                                            :question "The meaning of life?"}) 
                 => {:status ::http-api/success
                     :result {:_id QUESTION_ID
                              :objective-id OBJECTIVE_ID}}
                 (oauth/access-token anything anything anything) => {:user_id USER_ID} 
                 (http-api/create-user anything) => {:status ::http-api/success
                                                     :result {:_id USER_ID}}) 
               (let [params {:question "The meaning of life?"}
                     {response :response} (-> user-session
                                              (helpers/with-sign-in "http://localhost:8080/")
                                              (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID "/questions")
                                                         :request-method :post
                                                         :params params))]
                 (:flash response) => (contains {:type :share-question})
                 (:headers response) => (helpers/location-contains (str "/objectives/" OBJECTIVE_ID "#questions")))))

       (fact "Any user can view a question against an objective"
             (against-background
               (http-api/get-question OBJECTIVE_ID QUESTION_ID) => {:status ::http-api/success
                                                                    :result {:question "The meaning of life?"
                                                                             :created-by-id USER_ID
                                                                             :uri QUESTION_URI
                                                                             :objective-id OBJECTIVE_ID
                                                                             :_id QUESTION_ID}}
               (http-api/retrieve-answers QUESTION_URI) => {:status ::http-api/success
                                                                        :result []} 
               (http-api/get-objective OBJECTIVE_ID)=> {:status ::http-api/success 
                                                        :result {:title "some title"}})
             (default-app question-view-get-request) => (contains {:status 200})
             (default-app question-view-get-request) => (contains {:body (contains "The meaning of life?")})) 

       (fact "Answers are displayed when viewing a question, including writer notes when present"
             (against-background
               (http-api/get-question OBJECTIVE_ID QUESTION_ID) => {:status ::http-api/success
                                                                    :result {:question "The meaning of life?"
                                                                             :created-by-id USER_ID
                                                                             :uri QUESTION_URI
                                                                             :objective-id OBJECTIVE_ID
                                                                             :_id QUESTION_ID}}
               (http-api/retrieve-answers QUESTION_URI) => {:status ::http-api/success
                                                                        :result [{:_id 12
                                                                                  :objective-id OBJECTIVE_ID
                                                                                  :question-id QUESTION_ID
                                                                                  :created-by-id USER_ID
                                                                                  :answer "The answer"
                                                                                  :note "The writer note"
                                                                                  :votes {:up 1237 
                                                                                          :down 6601}}
                                                                                 {:_id 13
                                                                                  :objective-id OBJECTIVE_ID
                                                                                  :question-id QUESTION_ID
                                                                                  :created-by-id USER_ID
                                                                                  :answer "Another answer"
                                                                                  :votes {:up 0 :down 0}}]} 
               (http-api/get-objective OBJECTIVE_ID)=> {:status ::http-api/success 
                                                        :result {:title "some title"}})
             (default-app question-view-get-request) => (contains {:body (contains "The answer")})
             (default-app question-view-get-request) => (contains {:body (contains "The writer note")})
             (default-app question-view-get-request) => (contains {:body (contains "1237")})
             (default-app question-view-get-request) => (contains {:body (contains "6601")})
             (helpers/count-matches (:body (default-app question-view-get-request)) "clj-writer-note-item-container") => 1)

       (fact "A user should receive a 404 if a question doesn't exist"
             (against-background
               (http-api/get-question OBJECTIVE_ID QUESTION_ID) => {:status ::http-api/not-found})
             (default-app question-view-get-request) => (contains {:status 404})) 

       (tabular
         (fact "A user should see an error page when they attempt to access a question with non-integer ID's"
               (default-app (invalid-question-get-request ?objective_id ?question_id)) => (contains {:status 404}))
         ?objective_id  ?question_id
         INVALID_ID     QUESTION_ID
         OBJECTIVE_ID   INVALID_ID
         INVALID_ID     INVALID_ID)

       (fact "A user should be redirected to the objective page when they attempt to view the questions page for an objective"
             (let [response (default-app questions-view-get-request)
                   objective-url (utils/path-for :fe/objective :id OBJECTIVE_ID)]
               (:status response) => 302
               (get-in response [:headers "Location"]) => objective-url)))

(def MARK_ID 56)
(def USER_URI (str "/users/" USER_ID))
(def QUESTION_URI (str "/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))
(def OBJECTIVE_URL (str "/objectives/" OBJECTIVE_ID))
(def MARK_URI (str "/meta/marks/" MARK_ID))

(def user-owning-objective {:_id USER_ID :owned-objectives [{:_id OBJECTIVE_ID}]})

(facts "about marking questions"
       (binding [config/enable-csrf false]
         (fact "objective owners and writers can mark questions to enhance visibility"
               (against-background
                (oauth/access-token anything anything anything) => {:user_id USER_ID}
                (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                                :result user-owning-objective}
                (http-api/get-user anything) => {:result user-owning-objective})
               (against-background
                (http-api/post-mark {:created-by-uri USER_URI
                                     :question-uri QUESTION_URI}) => {:status ::http-api/success
                                                                      :result {:created-by-uri USER_URI
                                                                               :question-uri QUESTION_URI
                                                                               :uri MARK_URI
                                                                               :active true}})

               (let [params {:refer OBJECTIVE_URL
                             :question-uri QUESTION_URI}
                     {response :response} (-> user-session
                                              helpers/sign-in-as-existing-user
                                              (p/request (str "http://localhost:8080/meta/marks")
                                                         :request-method :post
                                                         :params params))]
                 (:status response) => 302
                 (:headers response) => (helpers/location-contains OBJECTIVE_URL)))))
