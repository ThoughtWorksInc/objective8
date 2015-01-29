(ns d-cent.api-test
  (:use org.httpkit.fake)
  (:require [org.httpkit.client :as http]
            [midje.sweet :refer :all]
            [d-cent.api :as api]
            [d-cent.utils :as utils]
            [cheshire.core :as json]))

(def host-url utils/host-url)

(def the-user-profile {:user-id "user-id"
                       :email-address "blah@blah.com"})

(def the-stored-user-profile (into the-user-profile {:_id "GUID"}))

(def profile-posts 
  {:successful {:status 201
                :headers {"Content-Type" "application/json"}
                :body (json/generate-string the-stored-user-profile)}
   :failure    {:status 500}})

(facts "about user profiles"
       (fact "returns stored profile when post succeeds"
             (with-fake-http [(str host-url "/api/v1/users") (:successful profile-posts)]
               (api/post-user-profile the-user-profile))
             => the-stored-user-profile)

       (fact "returns api-failure when post fails"
             (with-fake-http [(str host-url "/api/v1/users") (:failure profile-posts)]
               (api/post-user-profile the-user-profile))
             => api/api-failure))
