(ns d-cent.user-test
  (:require [midje.sweet :refer :all]
            [d-cent.user :refer :all]))

(def email-address "test@email.com")
(def user-id :twitter-1)


(fact "can retrieve email address from store by user-id"
      (find-email-address-for-user {user-id {:email-address email-address}} user-id) => email-address)

(fact "returns nil if user does not exist in store"
      (find-email-address-for-user {} user-id) => nil)

(fact "returns nil if user does not have email address in store"
      (find-email-address-for-user {user-id {}} user-id) => nil)




 









