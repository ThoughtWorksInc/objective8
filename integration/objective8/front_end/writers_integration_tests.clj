(ns objective8.front-end.writers-integration-tests
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.config :as config]
            [objective8.integration-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.http-api :as http-api]))

(def TWITTER_ID "twitter-ID")
(def USER_ID 1)
(def OBJECTIVE_ID 2)
(def OBJECTIVE_TITLE "some title")
(def INVITATION_ID 3)
(def UUID "random-uuid")
(def candidates-get-request (mock/request :get (str utils/host-url "/objectives/" OBJECTIVE_ID "/candidate-writers")))

(def default-app (core/app core/app-config))

(facts "about writers" :integration
       (binding [config/enable-csrf false]
         (fact "authorised user can invite a policy writer on an objective"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/create-user anything) => {:status ::http-api/success
                                                     :result {:_id USER_ID}}
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success}
                 (http-api/create-invitation {:writer-name "bob"
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

         (fact "A user should be able to view the candidate writers page for an objective"
               (against-background
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                           :result {:title "some title"
                                                                    :_id OBJECTIVE_ID}})
               (let [response (default-app candidates-get-request)]
                 response  => (contains {:status 200})
                 response)  => (contains {:body (contains "martina")})
               (provided
                 (http-api/retrieve-candidates OBJECTIVE_ID) => {:status ::http-api/success
                                                                 :result [{:user-id USER_ID
                                                                           :objective-id OBJECTIVE_ID
                                                                           :invitation-id INVITATION_ID
                                                                           :name "martina"
                                                                           :invitation-reason "she's expert"}]}))

         (fact "a user should receive a 404 if accessing the candidate writers page for an objective that does not exist"
               (against-background
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/not-found})

               (default-app candidates-get-request) => (contains {:status 404}))))

(facts "about responding to invitations" :integration
       (fact "an invited writer is redirected to the accept/reject page when accessing their invitation link"
             (against-background
               (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                               :result {:_id INVITATION_ID
                                                                        :invited-by-id USER_ID
                                                                        :objective-id OBJECTIVE_ID
                                                                        :uuid UUID
                                                                        :status "active"}}
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result {:title OBJECTIVE_TITLE}})
             (let [user-session (helpers/test-context)
                   invitation-url (str "http://localhost:8080/invitations/" UUID)
                   accept-reject-url (str "/objectives/" OBJECTIVE_ID "/invited-writers/" INVITATION_ID)
                   peridot-response (-> user-session
                                        (p/request invitation-url)
                                        p/follow-redirect)]
               peridot-response => (contains {:request (contains {:uri (contains accept-reject-url)})})
               peridot-response => (contains {:response (contains {:body (contains OBJECTIVE_TITLE)})})))

       (fact "an invitation url gives a 404 if the invitation doesn't exist"
             (against-background
               (http-api/retrieve-invitation-by-uuid anything) => {:status ::http-api/not-found})
             (p/request (helpers/test-context) "/invitations/nonexistent-invitation-uuid") => (contains {:response (contains {:status 404})}))

       (fact "a user cannot access the accept/reject page without invitation credentials"
             (p/request (helpers/test-context) (str "http://localhost:8080/objectives/" OBJECTIVE_ID "/invited-writers/" INVITATION_ID)) 
             => (contains {:response (contains {:status 404})}))
       
       (binding [config/enable-csrf false]
         (facts "accepting an invitation"
                (against-background
                  (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                  (http-api/create-user anything) => {:status ::http-api/success
                                                      :result {:_id USER_ID}})
                (fact "a user can accept an invitation when they have invitation credentials"
                      (against-background
                        (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                                        :result {:_id INVITATION_ID
                                                                                 :invited-by-id USER_ID
                                                                                 :objective-id OBJECTIVE_ID
                                                                                 :uuid UUID
                                                                                 :status "active"}}
                        (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                                  :result {:title OBJECTIVE_TITLE}})
                      (let [user-session (helpers/test-context)
                            invitation-url (str "http://localhost:8080/invitations/" UUID)
                            peridot-response (-> user-session
                                                 (helpers/with-sign-in "http://localhost:8080/")
                                                 (p/request (str "http://localhost:8080/invitations/" UUID))
                                                 (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID 
                                                                 "/invited-writers/" INVITATION_ID "/accept")
                                                            :request-method :post)
                                                 p/follow-redirect)]
                        peridot-response) => (contains {:request (contains {:uri (contains (str "/objectives/" OBJECTIVE_ID 
                                                                                               "/candidate-writers"))})})
                      (provided
                          (http-api/accept-invitation {:invitee-id USER_ID
                                                       :invitation-id INVITATION_ID
                                                       :objective-id OBJECTIVE_ID})=> {:status ::http-api/success
                                                                                       :result {}}))

                (fact "a user cannot accept an invitation without invitation credentials"
                      (let [user-session (helpers/test-context)
                            invitation-url (str "http://localhost:8080/invitations/" UUID)
                            peridot-response (-> user-session
                                                 (helpers/with-sign-in "http://localhost:8080/")
                                                 (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID 
                                                                 "/invited-writers/" INVITATION_ID "/accept")
                                                            :request-method :post))]
                        peridot-response => (contains {:response (contains {:status 401})}))))))
