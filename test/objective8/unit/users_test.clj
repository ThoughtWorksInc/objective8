(ns objective8.unit.users-test
  (:require [midje.sweet :refer :all]
            [objective8.back-end.domain.users :as users]
            [objective8.back-end.domain.writers :as writers]
            [objective8.back-end.domain.objectives :as objectives]
            [objective8.back-end.storage.storage :as storage]))

(def email-address "test@email.com")
(def twitter-id "twitter-1")
(def username "testname1")

(def USER_ID 1)
(def user-uri (str "/users/" USER_ID))

(def the-user {:auth-provider-user-id twitter-id
               :email-address email-address
               :username username})

(def user-entity (assoc the-user :entity :user))

(def stored-user (assoc user-entity :_id USER_ID))

(fact "Users can be stored"
      (users/store-user! the-user)
      => :stub-stored-user-profile

      (provided (storage/pg-store! user-entity)
                => :stub-stored-user-profile))

(facts "Retrieving users"
       (fact "can retrieve user from store by user-uri"
             (users/retrieve-user user-uri) => stored-user
             (provided (storage/pg-retrieve-entity-by-uri user-uri)
                       => stored-user))

       
       (fact "returns nil if no user exists matching user-uri"
             (users/retrieve-user "/users/0") => nil
             (provided (storage/pg-retrieve-entity-by-uri "/users/0")
                       => {:uri "/users/0"})))

(facts "Finding users by auth-provider-user-id"
       (fact "can find user by auth-provider-user-id"
             (users/find-user-by-auth-provider-user-id twitter-id) => stored-user
             (provided (storage/pg-retrieve {:entity :user :auth-provider-user-id twitter-id})
                       => {:query {:entity :user
                                   :auth-provider-user-id twitter-id}
                           :result [(assoc stored-user :entity :user)]}))
       (fact "returns nil if no user exists with given auth-provider-user-id"
             (users/find-user-by-auth-provider-user-id "twitter-00000") => nil
             (provided (storage/pg-retrieve {:entity :user :auth-provider-user-id "twitter-00000"})
                       => {:query {:entity :user
                                   :auth-provider-user-id "twitter-00000"}
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
