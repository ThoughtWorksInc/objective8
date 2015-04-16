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
               (can-mark-question-roles request) => #{:writer-for-1 :owner-of-1})))
