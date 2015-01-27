(ns d-cent.user-test
  (:require [midje.sweet :refer :all]
            [d-cent.user :refer :all]))

(def email-address "test@email.com")
(def user-id :twitter-1)

(facts "retrieve email"
       (fact "can retrieve email address from store by user-id"
             (find-email-address-for-user (atom {user-id {:email-address email-address}}) user-id) => email-address)

       (fact "returns nil if user does not exist in store"
             (find-email-address-for-user (atom {}) user-id) => nil)

       (fact "returns nil if user does not have email address in store"
             (find-email-address-for-user (atom {user-id {}}) user-id) => nil))


(defn stored-email-address-matches [user-id email-address]
  (fn [store]
    (= (find-email-address-for-user store user-id)
       email-address)))

(facts "store email"
       (fact "can store an email address for a user"
             (store-email-address-for-user! (atom {}) :the-user-id :the-email-address)
             => (stored-email-address-matches :the-user-id :the-email-address)))
