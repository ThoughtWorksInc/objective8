(ns objective8.front-end.writers-invite-integration-tests
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.config :as config] 
            [objective8.integration-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.core :as core]  
            [objective8.http-api :as http-api]))

(def USER_ID 1)
(def OBJECTIVE_ID 2)
(def INVITATION_ID 3)
(def UUID "random-uuid")
(def writers-invite-get-request (mock/request :get (str utils/host-url "/objectives/" OBJECTIVE_ID "/writers")))

(def default-app (core/app core/app-config))

(facts "about writers" :integration
       (binding [config/enable-csrf false]
         (fact "authorised user can invite a policy writer on an objective"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id USER_ID}
                 (http-api/create-user anything) => {:status ::http-api/success
                                                     :result {:_id USER_ID}}
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success}
                 (http-api/invite-writer {:writer-name "bob"
                                          :reason "he's awesome"
                                          :objective-id OBJECTIVE_ID
                                          :invited-by-id USER_ID}) => {:status ::http-api/success
                                                                          :result {:_id INVITATION_ID
                                                                                   :objective-id OBJECTIVE_ID
                                                                                   :uuid UUID}})
               (let [user-session (helpers/test-context)
                     params {:writer-name "bob"
                             :reason "he's awesome"}
                     peridot-response (-> user-session
                                          (helpers/with-sign-in "http://localhost:8080/")
                                          (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID "/writers/invitations")
                                                     :request-method :post
                                                     :params params))]
                 peridot-response => (helpers/flash-message-contains (str "Your invited writer can accept their invitation by going to http://localhost:8080/invitations/" UUID))
                 peridot-response => (helpers/headers-location (str "/objectives/" OBJECTIVE_ID))))

         (fact "A user should be able to view the invite writers page for an objective"
               (against-background
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                           :result {:title "some title" 
                                                                  :_id OBJECTIVE_ID}})
               (default-app writers-invite-get-request) => (contains {:status 200})))) 
