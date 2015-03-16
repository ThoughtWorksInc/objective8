(ns objective8.integration.front-end.writers
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.config :as config]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.handlers.front-end :as front-end]
            [objective8.http-api :as http-api]))

(def TWITTER_ID "twitter-ID")
(def USER_ID 1)
(def OBJECTIVE_ID 2)
(def writer-role-for-objective (keyword (str "writer-for-" OBJECTIVE_ID)))
(def OBJECTIVE_TITLE "some title")
(def INVITATION_ID 3)
(def UUID "random-uuid")
(def candidates-get-request (mock/request :get (str utils/host-url "/objectives/" OBJECTIVE_ID "/candidate-writers")))

(def invitation-url (str utils/host-url "/invitations/" UUID))
(def invitation-response-url (str utils/host-url "/objectives/" OBJECTIVE_ID "/writer-invitations/" INVITATION_ID))
(def accept-invitation-url (str invitation-response-url "/accept"))
(def decline-invitation-url (str invitation-response-url "/decline"))

(def active-invitation {:_id INVITATION_ID
                        :invited-by-id USER_ID
                        :objective-id OBJECTIVE_ID
                        :uuid UUID
                        :status "active"})

(def expired-invitation (assoc active-invitation :status "expired"))

(def default-app (core/app core/app-config))
(def user-session (helpers/test-context))

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
               (let [params {:writer-name "bob"
                             :reason "he's awesome"}
                     peridot-response (-> user-session
                                          (helpers/with-sign-in "http://localhost:8080/")
                                          (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID "/writer-invitations")
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
                                                                           :writer-name "martina"
                                                                           :invitation-reason "she's expert"}]}))

         (fact "a user should receive a 404 if accessing the candidate writers page for an objective that does not exist"
               (against-background
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/not-found})

               (default-app candidates-get-request) => (contains {:status 404}))))

(facts "about responding to invitations" :integration
       (fact "an invited writer is redirected to the accept/decline page when accessing their invitation link"
             (against-background
              (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                              :result {:_id INVITATION_ID
                                                                       :invited-by-id USER_ID
                                                                       :objective-id OBJECTIVE_ID
                                                                       :uuid UUID
                                                                       :status "active"}}
              (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                        :result {:title OBJECTIVE_TITLE}})
             (let [accept-decline-url (str "/objectives/" OBJECTIVE_ID "/writer-invitations/" INVITATION_ID)
                   peridot-response (-> user-session
                                        (p/request invitation-url)
                                        p/follow-redirect)]
               peridot-response => (contains {:request (contains {:uri (contains accept-decline-url)})})
               peridot-response => (contains {:response (contains {:body (contains OBJECTIVE_TITLE)})})))

       (fact "a user is redirected to the objective details page with a flash message if the invitation has expired"
             (against-background
              (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                              :result expired-invitation}
              (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                        :result {:title OBJECTIVE_TITLE}}
              (http-api/retrieve-comments anything) => {:status ::http-api/success
                                                        :result []})
             (let [{request :request response :response} (-> user-session
                                                             (p/request invitation-url)
                                                             p/follow-redirect)]
               (:uri request) => (str "/objectives/" OBJECTIVE_ID)
               (:body response) => (contains "This invitation has expired")))

       (fact "an invitation url gives a 404 if the invitation doesn't exist"
             (against-background
              (http-api/retrieve-invitation-by-uuid anything) => {:status ::http-api/not-found})
             (p/request user-session "/invitations/nonexistent-invitation-uuid") => (contains {:response (contains {:status 404})}))

       (fact "a user cannot access the accept/decline page without invitation credentials"
             (p/request user-session invitation-response-url)
             => (contains {:response (contains {:status 404})}))

       (fact "a user cannot access the accept/decline page with invitation credentials that don't match an active invitation" 
             (-> user-session
                 (p/request invitation-url)
                 p/follow-redirect)
             => anything
             (provided 
              (http-api/retrieve-invitation-by-uuid anything) =streams=> [{:status ::http-api/success
                                                                           :result {:_id INVITATION_ID
                                                                                    :invited-by-id USER_ID
                                                                                    :objective-id OBJECTIVE_ID
                                                                                    :uuid :NOT_AN_ACTIVE_UUID
                                                                                    :status "active"}} 
                                                                          {:status ::http-api/not-found}]
              (front-end/error-404-response anything) => {:status 404}
              (front-end/remove-invitation-credentials anything) => {})))


(binding [config/enable-csrf false]
  (facts "accepting an invitation"
         (against-background
          (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
          (http-api/create-user anything) => {:status ::http-api/success
                                              :result {:_id USER_ID}})
         (fact "a user can accept an invitation when they have invitation credentials and they're signed in"
               (against-background
                (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                                :result active-invitation}
                (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                          :result {:title OBJECTIVE_TITLE}})
               (let [{request :request} (-> user-session
                                            (helpers/with-sign-in "http://localhost:8080/")
                                            (p/request invitation-url)
                                            (p/request accept-invitation-url 
                                                       :request-method :post)
                                            p/follow-redirect)]
                 (:uri request)) => (contains (str "/objectives/" OBJECTIVE_ID "/candidate-writers"))
                 (provided
                  (http-api/post-candidate-writer {:invitee-id USER_ID
                                                   :invitation-uuid UUID
                                                   :objective-id OBJECTIVE_ID}) => {:status ::http-api/success
                                                                                    :result {}}))

         (fact "a user is granted writer-for-OBJECTIVE_ID role when accepting an invitation"
               (against-background
                (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                                :result active-invitation}
                (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                          :result {:title OBJECTIVE_TITLE}})

               (-> user-session
                   (helpers/with-sign-in "http://localhost:8080/")
                   (p/request invitation-url)
                   (p/request accept-invitation-url :request-method :post)) => anything
                   
                   (provided
                    (http-api/post-candidate-writer {:invitee-id USER_ID
                                                     :invitation-uuid UUID
                                                     :objective-id OBJECTIVE_ID})
                    => {:status ::http-api/success
                        :result {}}
                    (utils/add-authorisation-role anything writer-role-for-objective) => {}))
         
         
         (fact "a user cannot accept an invitation without invitation credentials"
               (let [peridot-response (-> user-session
                                          (helpers/with-sign-in "http://localhost:8080/")
                                          (p/request accept-invitation-url 
                                                     :request-method :post))]
                 peridot-response => (contains {:response (contains {:status 401})})))))

(binding [config/enable-csrf false]
  (facts "declining an invitation"
         (fact "a user can decline an invitation when they have invitation credentials"
               (against-background
                 (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                                 :result active-invitation})
               (let [peridot-response (-> user-session
                                          (p/request invitation-url)
                                          (p/request decline-invitation-url
                                                     :request-method :post)
                                          p/follow-redirect)]  
                 peridot-response) => (contains {:request (contains {:uri "/"})})
               (provided
                 (http-api/decline-invitation {:invitation-uuid UUID
                                               :objective-id OBJECTIVE_ID
                                               :invitation-id INVITATION_ID}) => {:status ::http-api/success
                                                                                  :result {}}))

         (fact "a user cannot decline an invitation without invitation credentials"
               (-> (p/request user-session decline-invitation-url :request-method :post)
                   (get-in [:response :status])) => 401)))
