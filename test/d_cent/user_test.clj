(ns d-cent.user-test
  (:require [midje.sweet :refer :all]
            [d-cent.user :refer :all]
            [d-cent.storage.storage :refer [store!]]))

(def email-address "test@email.com")
(def the-user-id :twitter-1)
(def the-user-record {:user-id the-user-id})

(facts "about creating user records"
       (fact "they can be stored"
             (let [user-profile {:user-id the-user-id :email-address email-address}]
               (store-user-profile! :stub-store user-profile)
               => :stub-stored-user-profile

               (provided (store! :stub-store "users" user-profile)
                         => :stub-stored-user-profile))))

(facts "retrieve user record"
       (fact "can retrieve user record from store by user-id"
             (let [store (atom {})]
               (store! store "users" the-user-record)
               (retrieve-user-record store the-user-id))
             => (contains the-user-record))

       (fact "returns nil if no user record exists matching user-id"
             (retrieve-user-record (atom {}) the-user-id)
             => nil))
