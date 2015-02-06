(ns d-cent.api.users-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [cheshire.core :as json]
            [d-cent.core :as core]
            [d-cent.storage.storage :as s]
            [d-cent.user :as user]
            [d-cent.integration-helpers :as helpers]))

;; Testing from http request -> making correct calls within user namespace
;; Mock or stub out 'user' namespace

(def email-address "test@email.address.com")
(def twitter-id "twitter-1")

(def test-db (atom {}))
(def app (helpers/test-context test-db))

(def USER_ID 10)

(def user {:twitter-id twitter-id
           :email-address email-address})

(def stored-user (assoc user :_id USER_ID))

(facts "about retrieving users by id"
       (fact "can retrieve a user by id"
             (let [peridot-response (p/request app (str "/api/v1/users/" USER_ID))]
               peridot-response) => (helpers/check-json-body stored-user)
               (provided
                (user/retrieve-user USER_ID) => stored-user))

       (fact "returns a 404 if a user does not exist"
             (against-background
              (user/retrieve-user anything) => nil)
             
             (p/request app (str "/api/v1/users/" 123456))
             => (contains {:response (contains {:status 404})}))

       (fact "returns a 400 (Bad request) if user id is not an integer"
             (p/request app "/api/v1/users/NOT-AN-INTEGER")
             => (contains {:response (contains {:status 400})})))

(facts "about querying for users"
 (fact "a user can be retrieved by twitter id"
       (let [peridot-response (p/request app (str "/api/v1/users?twitter=" twitter-id))]
         peridot-response) => (helpers/check-json-body stored-user)
         (provided (user/find-user-by-twitter-id twitter-id) => stored-user))

 (fact "returns a 404 if the user does not exist"
       (let [user-request (p/request app (str "/api/v1/users?twitter=twitter-IDONTEXIST"))]
         (-> user-request :response :status)) => 404
         (provided (user/find-user-by-twitter-id "twitter-IDONTEXIST") => nil)))

(facts "about posting users"
       (fact "the posted user is stored"
             (let [peridot-response (p/request app "/api/v1/users"
                                               :request-method :post
                                               :content-type "application/json"
                                               :body (json/generate-string user))]
               peridot-response)
             => (helpers/check-json-body stored-user)
             (provided
              (user/store-user! user) => stored-user))
       
       (fact "the http response indicates the location of the user"
             (against-background
              (user/store-user! anything) => stored-user)
             
             (let [result (p/request app "/api/v1/users"
                                     :request-method :post
                                     :content-type "application/json"
                                     :body (json/generate-string user))
                   response (:response result)
                   headers (:headers response)]
               response => (contains {:status 201})
               headers => (contains {"Location" (contains (str "/api/v1/users/" USER_ID))}))))
