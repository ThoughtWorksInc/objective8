(ns d-cent.api-test
  (:use org.httpkit.fake)
  (:require [org.httpkit.client :as http]
            [midje.sweet :refer :all]
            [d-cent.http-api :as api]
            [d-cent.utils :as utils]
            [cheshire.core :as json]))

(def host-url utils/host-url)

;USERS
(def the-user-profile {:user-id "user-id"
                       :email-address "blah@blah.com"})
(def the-stored-user-profile (into the-user-profile {:_id "GUID"}))
(def profile-posts
  {:successful {:status 201
                :headers {"Content-Type" "application/json"}
                :body (json/generate-string the-stored-user-profile)}
   :failure    {:status 500}})


;OBJECTIVES
(def the-objective {:title "My Objective"
                    :goals "To rock out, All day"
                    :description "I like cake"
                    :end-date "2015-01-31"
                    :username "my username"})

(def the-stored-objective (into the-objective {:_id "GUID"}))
(def objective-posts 
  {:successful {:status 201
                :headers {"Content-Type" "application/json"}
                :body (json/generate-string the-stored-objective)}
   :failure    {:status 500}})



(facts "about user profiles"
       (fact "returns stored profile when post succeeds"
             (with-fake-http [(str host-url "/api/v1/users") (:successful profile-posts)]
               (api/create-user-profile the-user-profile))
             => the-stored-user-profile)

       (fact "returns api-failure when post fails"
             (with-fake-http [(str host-url "/api/v1/users") (:failure profile-posts)]
               (api/create-user-profile the-user-profile))
             => api/api-failure))

(facts "about posting objectives"
       (fact "returns a stored objective when post succeeds"
          (with-fake-http [(str host-url "/api/v1/objectives") (:successful objective-posts)]
            (api/create-objective the-objective))
          => the-stored-objective)

    (fact "returns api-failure when post fails"
             (with-fake-http [(str host-url "/api/v1/objectives") (:failure objective-posts)]
               (api/create-objective the-objective))
             => api/api-failure))

(facts "about getting objectives"
       (fact "returns a stored objective when one exists with given id"
             (with-fake-http [(str host-url "/api/v1/objectives/" "OBJECTIVE_GUID")
                              {:status 200
                               :headers {"Content-Type" "application/json"}
                               :body (json/generate-string
                                      {:_id "OBJECTIVE_GUID"
                                       :title "Objective title"
                                       :goals "Objective goals"
                                       :description "Objective description"
                                       :end-date "2015-01-31T00:00:00.000Z"
                                       :user-guid "USER_GUID"})}]
               (api/get-objective "OBJECTIVE_GUID"))
             => (contains {:_id "OBJECTIVE_GUID"
                           :title "Objective title"
                           :goals "Objective goals"
                           :description "Objective description"
                           :end-date (utils/time-string->time-stamp "2015-01-31T00:00:00.000Z")
                           :user-guid "USER_GUID"
                           }))
       (fact "returns api-failure when no objective found"))
