(ns objective8.users-test
  (:require [midje.sweet :refer :all]
            [objective8.users :as users]
            [objective8.storage.storage :as storage]))

(def email-address "test@email.com")
(def twitter-id "twitter-1")
(def username "testname1")

(def USER_ID 1)

(def the-user {:twitter-id twitter-id
               :email-address email-address
               :username username})

(def user-entity (assoc the-user :entity :user))

(def stored-user (assoc the-user :_id USER_ID))

(fact "Users can be stored"
      (users/store-user! the-user)
      => :stub-stored-user-profile

      (provided (storage/pg-store! user-entity)
                => :stub-stored-user-profile))

(facts "Retrieving users"
       (fact "can retrieve user from store by user-id"
             (users/retrieve-user USER_ID) => stored-user
             (provided (storage/pg-retrieve {:entity :user :_id USER_ID})
                       => {:query {:entity :user
                                   :_id USER_ID}
                           :result [(assoc stored-user :entity :user)]}))

       (fact "returns nil if no user exists matching user-id"
             (users/retrieve-user 0) => nil
             (provided (storage/pg-retrieve {:entity :user :_id 0})
                       => {:query {:entity :user
                                   :_id 0}
                           :result []})))

(facts "Finding users by twitter-id"
       (fact "can find user by twitter-id"
             (users/find-user-by-twitter-id twitter-id) => stored-user
             (provided (storage/pg-retrieve {:entity :user :twitter-id twitter-id})
                       => {:query {:entity :user
                                   :twitter-id twitter-id}
                           :result [(assoc stored-user :entity :user)]}))
       (fact "returns nil if no user exists with given twitter-id"
             (users/find-user-by-twitter-id "twitter-00000") => nil
             (provided (storage/pg-retrieve {:entity :user :twitter-id "twitter-00000"})
                       => {:query {:entity :user
                                   :twitter-id "twitter-00000"}
                           :result []})))

(facts "Finding users by username"
       (fact "can find user by username"
             (users/find-user-by-username username) => stored-user
             (provided (storage/pg-retrieve {:entity :user :username username})
                       => {:query {:entity :user
                                   :username username}
                           :result [(assoc stored-user :entity :user)]}))
       (fact "returns nil if no user exists with given username"
             (users/find-user-by-username "NotaUsername") => nil
             (provided (storage/pg-retrieve {:entity :user :username "NotaUsername"})
                       => {:query {:entity :user
                                   :username "NotaUsername"}
                           :result []})))
