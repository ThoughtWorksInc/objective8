(ns objective8.unit.workflows.sign-up-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [objective8.front-end.workflows.sign-up :refer :all]
            [objective8.front-end.api.http :as http-api]
            [objective8.utils :as utils]))

(fact "retain invitation information in session when signing in / signing up with redirect"
      (let [session-with-invitation {:invitation :invitation-data}]
        (authorised-redirect :user :some-url session-with-invitation)) => :authorised-response
        (provided
         (authorise (contains {:session (contains {:invitation :invitation-data})}) :user) => :authorised-response))

(def USER_ID 1)
(def the-user {:_id USER_ID})

(fact "about user assigning user roles"
      (fact "All signed in users obtain the :signed-in role"
            (against-background
             (http-api/get-user USER_ID) => {:writer-records []})
            (roles-for-user the-user) => (contains :signed-in))

      (fact "Users that are writers for objectives are assigned the relevant roles for those objectives"
            (against-background
             (http-api/get-user USER_ID) => {:status ::http-api/success
                                             :result {:writer-records [{:objective-id 1} {:objective-id 2}]}})
            (roles-for-user the-user) => (contains #{:writer-for-1 :writer-for-2}))

      (fact "Users are owners of the objectives they created"
            (against-background
             (http-api/get-user USER_ID) => {:status ::http-api/success
                                             :result {:owned-objectives [{:_id 1} {:_id 2}]}})
            (roles-for-user the-user) => (contains #{:owner-of-1 :owner-of-2}))
      
      (fact "Users that are writer-inviters for objectives are assigned the relevant roles for those objectives"
            (against-background
              (http-api/get-user USER_ID) => {:status ::http-api/success
                                              :result {:owned-objectives [{:_id 1} {:_id 2}]}})
            (roles-for-user the-user) => (contains #{:writer-inviter-for-1 :writer-inviter-for-2}))
      
      (fact "Users who have admin status have admin role added on sign-in"
            (against-background
              (http-api/get-user USER_ID) => {:status ::http-api/success
                                              :result {:admin true}})
            (roles-for-user the-user) => (contains #{:admin})))
