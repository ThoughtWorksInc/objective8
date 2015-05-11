(ns objective8.integration.front-end.writers
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.config :as config]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.permissions :as permissions]
            [objective8.core :as core]
            [objective8.handlers.front-end :as front-end]
            [objective8.http-api :as http-api]))

(def TWITTER_ID "twitter-ID")
(def USER_ID 1)
(def OBJECTIVE_ID 2)
(def WRITER_ROLE_FOR_OBJECTIVE (keyword (str "writer-for-" OBJECTIVE_ID)))
(def WRITER_INVITER_ROLE_FOR_OBJECTIVE (keyword (str "writer-inviter-for-" OBJECTIVE_ID)))
(def OBJECTIVE_TITLE "some title")
(def OBJECTIVE_URL (utils/local-path-for :fe/objective :id OBJECTIVE_ID)) 
(def INVITATION_ID 3)
(def UUID "random-uuid")
(def WRITER_EMAIL "writer@email.com")
(def writers-get-request (mock/request :get (utils/path-for :fe/writers-list :id OBJECTIVE_ID)))

(def INVITATION_URL (utils/path-for :fe/writer-invitation :uuid UUID))
(def ACCEPT_INVITATION_URL (utils/path-for :fe/accept-invitation :id OBJECTIVE_ID :i-id INVITATION_ID))
(def DECLINE_INVITATION_URL (utils/path-for :fe/decline-invitation :id OBJECTIVE_ID :i-id INVITATION_ID))
(def CREATE_PROFILE_URL (utils/path-for :fe/create-profile-get))

(def ACTIVE_INVITATION {:_id INVITATION_ID
                        :invited-by-id USER_ID
                        :objective-id OBJECTIVE_ID
                        :uuid UUID
                        :status "active"})

(def EXPIRED_INVITATION (assoc ACTIVE_INVITATION :status "expired"))

(def default-app (core/app helpers/test-config))
(def user-session (helpers/test-context))

(def writer-for-objective {:_id USER_ID :username "username" :writer-records [{:objective-id OBJECTIVE_ID}]})

(facts "about the invite writer form"
       (binding [config/enable-csrf false]
         
         
         (tabular
          (fact "validation errors are reported"
                (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                                 :result writer-for-objective}
                 (http-api/get-user anything) => {:result writer-for-objective}
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success})
                (-> user-session
                    helpers/sign-in-as-existing-user
                    (p/request (utils/path-for :fe/invitation-form-post :id OBJECTIVE_ID)
                               :request-method :post
                               :params {:writer-name ?writer-name
                                        :writer-email ?writer-email
                                        :reason ?reason})
                    p/follow-redirect
                    :response
                    :body) => (contains ?expected-error-message))
          
          ?writer-name                     ?writer-email   ?reason                           ?expected-error-message
          ""                               "a@b.com"       "a reason"                        "clj-writer-name-empty-error"
          (helpers/string-of-length 51)    "a@b.com"       "a reason"                        "clj-writer-name-length-error"
          "Jenny"                          ""              "a reason"                        "clj-writer-email-empty-error"
          "Jenny"                          "invalid-email" "a reason"                        "clj-writer-email-invalid-error"
          "Jenny"                          "a@b.com"       ""                                "clj-writer-reason-empty-error"
          "Jenny"                          "a@b.com"       (helpers/string-of-length 5001)   "clj-writer-reason-length-error")))

(facts "about writers"
       (binding [config/enable-csrf false]
         (fact "the objective owner can invite a policy writer on an objective"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/create-user anything) => {:status ::http-api/success
                                                     :result {:_id USER_ID
                                                              :username "username"}}
                 (http-api/get-user anything) => {:result {:owned-objectives [{:_id OBJECTIVE_ID}]}}
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success}
                 (http-api/create-invitation 
                   {:writer-name "bob"
                    :writer-email WRITER_EMAIL
                    :reason "he's awesome"
                    :objective-id OBJECTIVE_ID
                    :invited-by-id USER_ID}) => {:status ::http-api/success
                                                 :result {:_id INVITATION_ID
                                                          :objective-id OBJECTIVE_ID
                                                          :uuid UUID
                                                          :writer-email WRITER_EMAIL}})
               (let [params {:writer-name "bob"
                             :writer-email WRITER_EMAIL
                             :reason "he's awesome"}
                     peridot-response (-> user-session
                                          (helpers/with-sign-in "http://localhost:8080/")
                                          (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID "/writer-invitations")
                                                     :request-method :post
                                                     :params params))]
                 (:flash (:response peridot-response)) => 
                 {:type :invitation
                  :writer-email WRITER_EMAIL 
                  :invitation-url INVITATION_URL}
                 peridot-response => (helpers/headers-location (str "/objectives/" OBJECTIVE_ID))))

         (fact "an existing writer can invite a policy writer on an objective"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                                 :result {:_id USER_ID
                                                                          :username "username"}}
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success}
                 (http-api/get-user anything) => {:result {:writer-records [{:objective-id OBJECTIVE_ID}]}}

                 (http-api/create-invitation 
                   {:writer-name "bob"
                    :writer-email WRITER_EMAIL
                    :reason "he's awesome"
                    :objective-id OBJECTIVE_ID
                    :invited-by-id USER_ID}) => {:status ::http-api/success
                                                 :result {:_id INVITATION_ID
                                                          :objective-id OBJECTIVE_ID
                                                          :uuid UUID
                                                          :writer-email WRITER_EMAIL}})
               (let [params {:writer-name "bob"
                             :writer-email WRITER_EMAIL
                             :reason "he's awesome"}
                     peridot-response (-> user-session
                                          helpers/sign-in-as-existing-user 
                                          (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID "/writer-invitations")
                                                     :request-method :post
                                                     :params params))]
                 (:flash (:response peridot-response)) => 
                 {:type :invitation
                  :writer-email WRITER_EMAIL 
                  :invitation-url INVITATION_URL}
                 peridot-response => (helpers/headers-location (str "/objectives/" OBJECTIVE_ID))))

         (fact "an unauthorised user can not invite a policy writer on an objective"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/create-user anything) => {:status ::http-api/success
                                                     :result {:_id USER_ID
                                                              :username "username"}}
                 (http-api/get-user anything) => {:result {:writer-records [{:objective-id (inc OBJECTIVE_ID)}]}}
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success}
                 (http-api/create-invitation 
                   {:writer-name "bob"
                    :writer-email WRITER_EMAIL
                    :reason "he's awesome"
                    :objective-id OBJECTIVE_ID
                    :invited-by-id USER_ID}) => {:status ::http-api/success
                                                 :result {:_id INVITATION_ID
                                                          :objective-id OBJECTIVE_ID
                                                          :uuid UUID
                                                          :writer-email WRITER_EMAIL}})
               (let [params {:writer-name "bob"
                             :writer-email WRITER_EMAIL
                             :reason "he's awesome"}] 
                 (-> user-session
                     (helpers/with-sign-in "http://localhost:8080/")
                     (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID "/writer-invitations")
                                :request-method :post
                                :params params)
                 (get-in [:response :status])) => 403))

         (fact "A user should be redirected to objective page when attempting to view the writers page for an objective"
               (let [response (default-app writers-get-request)
                     objective-url (utils/path-for :fe/objective :id OBJECTIVE_ID)] 
                 (:status response) => 302 
                 (get-in response [:headers "Location"]) => objective-url))))

(facts "about responding to invitations"

       (fact "an invited writer is redirected to the objective page when accessing their invitation link"
             (against-background
               (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                               :result {:_id INVITATION_ID
                                                                        :invited-by-id USER_ID
                                                                        :objective-id OBJECTIVE_ID
                                                                        :uuid UUID
                                                                        :status "active"}}
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result {:title OBJECTIVE_TITLE
                                                                  :uri :objective-uri}}
               (http-api/retrieve-writers OBJECTIVE_ID) => {:status ::http-api/success :result []} 
               (http-api/retrieve-questions OBJECTIVE_ID) => {:status ::http-api/success :result []} 
               (http-api/get-comments anything) => {:status ::http-api/success
                                                    :result []}) 
             (let [peridot-response (-> user-session
                                        (p/request INVITATION_URL)
                                        p/follow-redirect)]
               peridot-response => (contains {:request (contains {:uri (contains OBJECTIVE_URL)})})
               peridot-response => (contains {:response (contains {:body (contains OBJECTIVE_TITLE)})})))

       (fact "an invited writer is shown a flash banner message with a link to the objective when navigating away from the objective (e.g. learn-more page)"
             (against-background
               (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                               :result {:_id INVITATION_ID
                                                                        :invited-by-id USER_ID
                                                                        :objective-id OBJECTIVE_ID
                                                                        :uuid UUID
                                                                        :status "active"}}
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result {:title OBJECTIVE_TITLE
                                                                  :uri :objective-uri}}
               (http-api/retrieve-writers OBJECTIVE_ID) => {:status ::http-api/success :result []} 
               (http-api/retrieve-questions OBJECTIVE_ID) => {:status ::http-api/success :result []} 
               (http-api/get-comments anything) => {:status ::http-api/success
                                                    :result []}) 
             (let [peridot-response (-> user-session
                                        (p/request INVITATION_URL)
                                        p/follow-redirect
                                        (p/request (utils/path-for :fe/learn-more)))]
               peridot-response => (contains {:response (contains {:body (contains (str "href=\"" OBJECTIVE_URL))})})))

       (fact "a user is redirected to the objective details page with a flash message if the invitation has expired"
             (against-background
               (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                               :result EXPIRED_INVITATION}
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result {:title OBJECTIVE_TITLE
                                                                  :uri :objective-uri}}
               (http-api/retrieve-writers OBJECTIVE_ID) => {:status ::http-api/success :result []}
               (http-api/retrieve-questions OBJECTIVE_ID) => {:status ::http-api/success :result []}
               (http-api/get-comments anything) => {:status ::http-api/success
                                                    :result []})
             (let [{request :request response :response} (-> user-session
                                                             (p/request INVITATION_URL)
                                                             p/follow-redirect)]
               (:uri request) => OBJECTIVE_URL
               (:body response) => (contains "This invitation has expired")))

       (fact "an invitation url returns a 404 if the invitation doesn't exist"
             (against-background
               (http-api/retrieve-invitation-by-uuid anything) => {:status ::http-api/not-found})
             (p/request user-session "/invitations/nonexistent-invitation-uuid") => (contains {:response (contains {:status 404})}))

       (fact "a user's invitation credentials are removed from the session when accessing the objective page with invitation credentials that don't match an active invitation" 
             (-> user-session
                 (p/request INVITATION_URL)
                 p/follow-redirect)
             => anything
             (provided 
               (http-api/retrieve-invitation-by-uuid anything) 
               =streams=> [{:status ::http-api/success
                            :result {:_id INVITATION_ID
                                     :invited-by-id USER_ID
                                     :objective-id OBJECTIVE_ID
                                     :uuid :NOT_AN_ACTIVE_UUID
                                     :status "active"}} 
                           {:status ::http-api/not-found}]
               (front-end/remove-invitation-from-session anything) => {})))


(binding [config/enable-csrf false]
  (facts "accepting an invitation"
         (against-background
           (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
           (http-api/create-user anything) => {:status ::http-api/success
                                               :result {:_id USER_ID}})

         (fact "an invited writer can reach the create profile page"
               (against-background
                 (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                                 :result ACTIVE_INVITATION}
                 (http-api/get-objective OBJECTIVE_ID anything) => {:status ::http-api/success
                                                           :result {:title OBJECTIVE_TITLE}})
               (let [{response :response} (-> user-session
                                              (helpers/with-sign-in "http://localhost:8080/")
                                              (p/request INVITATION_URL)
                                              (p/request CREATE_PROFILE_URL))]
                 (:status response)  => 200 
                 (:body response) => (contains "Create profile | Objective[8]")))

         (fact "an invited writer can create a profile which, in turn, accepts their invitation"
               (against-background
                 (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                                 :result ACTIVE_INVITATION}
                 (http-api/get-objective OBJECTIVE_ID anything) => {:status ::http-api/success
                                                           :result {:title OBJECTIVE_TITLE}})
               (let [{request :request} (-> user-session
                                            (helpers/with-sign-in "http://localhost:8080/")
                                            (p/request INVITATION_URL)
                                            (p/request CREATE_PROFILE_URL)
                                            (p/request CREATE_PROFILE_URL
                                                       :request-method :post
                                                       :params {:name "John Doe" :biog "My biog"})
                                            p/follow-redirect)]
                 (:uri request))  => (contains OBJECTIVE_URL) 
                 (provided
                   (http-api/post-profile {:name "John Doe"
                                           :biog "My biog"
                                           :user-uri (str "/users/" USER_ID)}) => {:status ::http-api/success
                                                                                   :result {}}
                   (http-api/get-user USER_ID) => {:status ::http-api/success
                                                   :result {:profile {:name "John Doe"
                                                                      :biog "My biog"}}}

                   (http-api/post-writer {:invitee-id USER_ID
                                          :invitation-uuid UUID
                                          :objective-id OBJECTIVE_ID}) => {:status ::http-api/success
                                                                           :result {}}))

         (fact "a user gets a 401 error response if they post to create profile without an invitation in their session"
               (let [{response :response} (-> user-session
                                              (helpers/with-sign-in "http://localhost:8080/")
                                              (p/request CREATE_PROFILE_URL
                                                         :request-method :post
                                                         :params {:name "John Doe" :biog "My biog"}))]
                 (:status response) => 401))

         (fact "a user gets a 500 error response if posting the profile to the API fails" 
               (against-background
                 (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                                 :result ACTIVE_INVITATION}
                 (http-api/get-objective OBJECTIVE_ID anything) => {:status ::http-api/success
                                                                    :result {:title OBJECTIVE_TITLE}})
               (let [{response :response} (-> user-session
                                              (helpers/with-sign-in "http://localhost:8080/")
                                              (p/request INVITATION_URL)
                                              (p/request CREATE_PROFILE_URL
                                                         :request-method :post
                                                         :params {:name "John Doe" :biog "My biog"}))]
                 (:status response)) => 500
               (provided
                 (http-api/post-profile {:name "John Doe"
                                         :biog "My biog"
                                         :user-uri (str "/users/" USER_ID)}) => {:status ::http-api/error
                                                                                 :result {}}))

         (fact "a user can accept an invitation when they have invitation credentials and they're signed in"
               (against-background
                 (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                                 :result ACTIVE_INVITATION}
                 (http-api/get-objective OBJECTIVE_ID anything) => {:status ::http-api/success
                                                           :result {:title OBJECTIVE_TITLE}})
               (let [{request :request} (-> user-session
                                            (helpers/with-sign-in "http://localhost:8080/")
                                            (p/request INVITATION_URL)
                                            (p/request ACCEPT_INVITATION_URL 
                                                       :request-method :post)
                                            p/follow-redirect)]
                 (:uri request)) => (contains OBJECTIVE_URL)
               (provided
                 (http-api/get-user USER_ID) => {:status ::http-api/success
                                                 :result {:profile {:name "John Doe"
                                                                    :biog "My biog"}}}
                 (http-api/post-writer {:invitee-id USER_ID
                                        :invitation-uuid UUID
                                        :objective-id OBJECTIVE_ID}) => {:status ::http-api/success
                                                                         :result {}}))

         (def user-with-writer-credentials {:_id USER_ID :username "username" :writer-records [{:objective-id OBJECTIVE_ID}]})

         (fact "a user cannot accept an invitation when they are already a writer for the same objective" 
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id USER_ID}
                 (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                                 :result user-with-writer-credentials} 
                 (http-api/get-user anything) => {:result user-with-writer-credentials}  
                 (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                                 :result ACTIVE_INVITATION}
                 (http-api/get-objective OBJECTIVE_ID anything) => {:status ::http-api/success
                                                                    :result {:title OBJECTIVE_TITLE}})
               (let [{response :response}  (-> user-session
                                              helpers/sign-in-as-existing-user
                                              (p/request INVITATION_URL)
                                              (p/request ACCEPT_INVITATION_URL :request-method :post)
                                              p/follow-redirect)]
                 (:status response)) => 200 
               (provided
                 (http-api/decline-invitation {:invitation-uuid UUID
                                               :objective-id OBJECTIVE_ID
                                               :invitation-id INVITATION_ID}) => {:status ::http-api/success}
                 (http-api/post-writer anything) => :not-called :times 0))

         (fact "a user is granted writer-for-OBJECTIVE_ID and writer-inviter-for-OBJECTIVE_ID roles when accepting an invitation"
               (against-background
                 (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                                 :result ACTIVE_INVITATION}
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                           :result {:title OBJECTIVE_TITLE}})

               (-> user-session
                   (helpers/with-sign-in "http://localhost:8080/")
                   (p/request INVITATION_URL)
                   (p/request ACCEPT_INVITATION_URL :request-method :post)) => anything

               (provided
                 (http-api/get-user USER_ID) => {:status ::http-api/success
                                                 :result {:profile {:name "John Doe"
                                                                    :biog "My biog"}}}
                 (http-api/post-writer {:invitee-id USER_ID
                                        :invitation-uuid UUID
                                        :objective-id OBJECTIVE_ID}) => {:status ::http-api/success
                                                                         :result {}}
                 (permissions/add-authorisation-role anything WRITER_ROLE_FOR_OBJECTIVE) => {}
                 (permissions/add-authorisation-role anything WRITER_INVITER_ROLE_FOR_OBJECTIVE) => {}))


         (fact "a user cannot accept an invitation without invitation credentials"
               (let [peridot-response (-> user-session
                                          (helpers/with-sign-in "http://localhost:8080/")
                                          (p/request ACCEPT_INVITATION_URL 
                                                     :request-method :post))]
                 peridot-response => (contains {:response (contains {:status 401})})))))


(binding [config/enable-csrf false]
  (facts "declining an invitation"
         (fact "a user can decline an invitation when they have invitation credentials"
               (against-background
                 (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                                 :result ACTIVE_INVITATION})
               (let [peridot-response (-> user-session
                                          (p/request INVITATION_URL)
                                          (p/request DECLINE_INVITATION_URL
                                                     :request-method :post)
                                          p/follow-redirect)]  
                 peridot-response) => (contains {:request (contains {:uri "/"})})
               (provided
                 (http-api/decline-invitation {:invitation-uuid UUID
                                               :objective-id OBJECTIVE_ID
                                               :invitation-id INVITATION_ID}) => {:status ::http-api/success
                                                                                  :result {}}))

         (fact "a user cannot decline an invitation without invitation credentials"
               (-> (p/request user-session DECLINE_INVITATION_URL :request-method :post)
                   (get-in [:response :status])) => 401)))

(binding [config/enable-csrf false]
  (facts "about editing a writer profile"
         (fact "a writer can reach the page to edit their profile" 
               (against-background 
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID} 
                 (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success :result user-with-writer-credentials} 
                 (http-api/get-user anything) => {:status ::http-api/success
                                                  :result {:profile {:name "real name"
                                                                     :biog "my existing biography"}
                                                           :writer-records [{:objective-id OBJECTIVE_ID}]}})
               (let [{response :response} (-> user-session
                                              helpers/sign-in-as-existing-user 
                                              (p/request (utils/path-for :fe/edit-profile-get)))]
                 (:status response) => 200
                 (:body response) => (contains "my existing biography")))

         (fact "a user who is not a writer can not reach the page to edit their profile"
               (against-background 
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID} 
                 (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                                 :result {:_id USER_ID
                                                                          :username "username"}}) 
               (let [peridot-response (-> user-session
                                          helpers/sign-in-as-existing-user 
                                          (p/request (utils/path-for :fe/edit-profile-get)))]
                 (:response peridot-response)) => (contains {:status 401}))
         
         (fact "a writer can edit their profile"
               (against-background 
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID} 
                 (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success :result user-with-writer-credentials}
                 (http-api/get-user anything) => {:status ::http-api/success
                                                  :result {:profile {:name "real name"
                                                                     :biog "my existing biography"}
                                                           :writer-records [{:objective-id OBJECTIVE_ID}]}}) 
               (let [{response :response} (-> user-session
                                              helpers/sign-in-as-existing-user 
                                              (p/request (utils/path-for :fe/edit-profile-post)
                                                         :request-method :post
                                                         :params {:name "My new name" :biog "My updated biog"}))]
                 
                 (get-in response [:headers "Location"])) => (utils/path-for :fe/profile :username "username")
               (provided
                 (http-api/post-profile {:name "My new name"
                                         :biog "My updated biog"
                                         :user-uri (str "/users/" USER_ID)}) => {:status ::http-api/success
                                                                                 :result {}}))
         
         (fact "a user who is not a writer can not edit profile"
               (against-background 
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID} 
                 (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success 
                                                                 :result {:_id USER_ID}}) 
                (let [{response :response} (-> user-session
                                               helpers/sign-in-as-existing-user 
                                               (p/request (utils/path-for :fe/edit-profile-post)
                                                          :request-method :post
                                                          :params {:name "My new name" :biog "My updated biog"}))]
                  (:status response) => 401))))
