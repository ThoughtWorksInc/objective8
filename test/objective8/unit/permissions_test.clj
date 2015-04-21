(ns objective8.unit.permissions-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [objective8.permissions :refer :all]))

(facts "about add-authorisation-role"
       (fact "adds a new role to an already-authenticated user's roles"
             (let [an-auth-map (workflows/make-auth {:username "user" :roles #{:a-role}})
                   request-with-some-auth (friend/merge-authentication {} an-auth-map)
                   authorised-response (add-authorisation-role request-with-some-auth :a-different-role)]
               (friend/authorized? #{:a-different-role} (friend/identity authorised-response)) => truthy))

       (fact "does nothing if the user is not already authenticated"
             (add-authorisation-role {} :a-role) => {}))

(facts "about roles for marking questions"
       (fact "writers and owners are authorized to mark questions"
             (let [request {:params {:question-uri "/objectives/1/questions/2"}}]
               (mark-request->mark-question-roles request) => #{:writer-for-1 :owner-of-1})))

(facts "about writer permissions"
       (fact "user is a writer if they have any writer-for permissions"
             (writer? {:roles #{:writer-for-1}}) => truthy)
       (fact "user is a writer if they have any owner-of permissions"
             (writer? {:roles #{:owner-of-1}}) => truthy)
       (fact "user is not a writer if they have no roles"
             (writer? {:username "barry"}) => falsey)
       (fact "user is not a writer if they don't have writer-for or owner-of permissions"
             (writer? {:username "barry" :roles #{:wibble}}) => falsey))  
