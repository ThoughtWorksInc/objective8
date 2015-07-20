(ns objective8.integration.back-end.users
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [cheshire.core :as json]
            [objective8.core :as core]
            [objective8.utils :as utils]
            [objective8.back-end.storage.storage :as s]
            [objective8.back-end.domain.users :as users]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.middleware :as m]))

(def email-address "test@email.address.com")
(def auth-provider-user-id "twitter-TWITTER_ID")
(def username "testname1")

(def app (helpers/api-context))

(def USER_ID 10)

(def user {:auth-provider-user-id auth-provider-user-id
           :email-address email-address
           :username username })

(def stored-user (assoc user :_id USER_ID))

(facts "GET /api/v1/users/:id"
       (against-background
        (m/valid-credentials? anything anything anything) => true)
       (against-background
        [(before :contents (do
                             (helpers/db-connection)
                             (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "retrieves the user record and associated writer records and owned objectives records by user id"
              (let [{user-id :_id :as the-user} (sh/store-a-user)
                    {owned-objective-id :_id} (sh/store-an-objective {:user the-user})
                    writer-record-1 (sh/store-a-writer {:user the-user})
                    writer-record-2 (sh/store-a-writer {:user the-user})
                    {response :response} (p/request app (utils/api-path-for :api/get-user :id user-id))]
                (:body response) =>
                (helpers/json-contains {:writer-records (contains [(contains writer-record-1) (contains writer-record-2)])})
                (:body response) =>
                (helpers/json-contains {:owned-objectives (contains [(contains {:_id owned-objective-id})])})
                (:body response) =>
                (helpers/json-contains {:admin false})))
 
         (fact "returns a 404 if the user does not exist"
               (-> (p/request app (utils/api-path-for :api/get-user :id 123456))
                   (get-in [:response :status])) => 404))
       
       (fact "retrieves user record with admin role for admin user"
             (let [{user-id :_id auth-provider-user-id :auth-provider-user-id :as the-user} (sh/store-a-user)
                   _ (users/store-admin! {:twitter-id auth-provider-user-id})
                   {response :response} (p/request app (utils/api-path-for :api/get-user :id user-id))]
               (:body response) => (helpers/json-contains {:admin true}))))

(facts "users"
       (facts "about querying for users"
              (against-background
                (m/valid-credentials? anything anything anything) => true)
              (against-background 
                [(before :contents (do
                                     (helpers/db-connection)
                                     (helpers/truncate-tables)))
                 (after :facts (helpers/truncate-tables))] 

                (fact "a user can be retrieved by auth-provider-user-id"
                      (let [{auth-provider-user-id :auth-provider-user-id :as the-user} (sh/store-a-user)
                            peridot-response (p/request app (str (utils/api-path-for :api/get-user-by-query) 
                                                                 "?auth_provider_user_id=" auth-provider-user-id))
                            body (get-in peridot-response [:response :body])]
                        body => (helpers/json-contains the-user))) 

                (fact "returns a 404 if the user does not exist"
                      (let [user-request (p/request app (str (utils/api-path-for :api/get-user-by-query) 
                                                             "?auth_provider_user_id=twitter-IDONTEXIST"))]
                        (-> user-request :response :status)) => 404) 

                (fact "a user can be retrieved by username"
                      (let [{username :username :as the-user} (sh/store-a-user)
                            peridot-response (p/request app (str (utils/api-path-for :api/get-user-by-query) "?username=" username)) 
                            body (get-in peridot-response [:response :body])]
                        body => (helpers/json-contains the-user)))))

       (facts "about posting users"
              (against-background
                (m/valid-credentials? anything anything anything) => true)
              (fact "the posted user is stored"
                    (let [peridot-response (p/request app (utils/api-path-for :api/post-user-profile)
                                                      :request-method :post
                                                      :content-type "application/json"
                                                      :body (json/generate-string user))]
                      peridot-response)
                    => (helpers/check-json-body stored-user)
                    (provided
                      (users/store-user! user) => stored-user))

              (fact "the http response indicates the location of the user"
                    (against-background
                      (users/store-user! anything) => stored-user)

                    (let [result (p/request app (utils/api-path-for :api/post-user-profile)
                                            :request-method :post
                                            :content-type "application/json"
                                            :body (json/generate-string user))
                          response (:response result)
                          headers (:headers response)]
                      response => (contains {:status 201})
                      headers => (contains {"Location" (contains (str "/api/v1/users/" USER_ID))})))

              (fact "a 400 status is returned if a PSQLException is raised"
                    (against-background
                      (users/store-user! anything) =throws=> (org.postgresql.util.PSQLException. 
                                                               (org.postgresql.util.ServerErrorMessage. "" 0)))
                    (:response (p/request app (utils/api-path-for :api/post-user-profile)
                                          :request-method :post
                                          :content-type "application/json"
                                          :body (json/generate-string user))) => (contains {:status 400})))
       
       (facts "about posting writer profiles"
              (against-background
                (m/valid-credentials? anything anything anything) => true)
              (fact "the writer profile info is updated in a user"
                    (let [{user-id :_id :as user} (sh/store-a-user)
                          {response :response} (p/request app (utils/api-path-for :api/put-writer-profile)
                                                          :request-method :put
                                                          :content-type "application/json"
                                                          :body (json/generate-string {:name "name" 
                                                                                       :biog "biography" 
                                                                                       :user-uri (str "/users/" user-id)}))]
                      (:status response) => 200
                      (:headers response) => (helpers/location-contains (str "/api/v1/users/" user-id))
                      (:body response) => (helpers/json-contains {:profile {:name "name" :biog "biography"}})))

              (fact "returns a 404 if the user does not exist"
                    (let [invalid-user-id 2323
                          {response :response} (p/request app (utils/api-path-for :api/put-writer-profile)
                                                          :request-method :put
                                                          :content-type "application/json"
                                                          :body (json/generate-string {:name "name"
                                                                                       :biog "biography"
                                                                                       :user-uri (str "/users/" invalid-user-id)}))]
                      (:status response) => 404))

              (fact "returns a 400 when posting invalid profile data"
                    (let [profile-data-without-biog {:name "name"
                                                   :user-uri (str "/users/" USER_ID)}
                          {response :response} (p/request app (utils/api-path-for :api/put-writer-profile)
                                                          :request-method :put
                                                          :content-type "application/json"
                                                          :body (json/generate-string profile-data-without-biog))]
                      (:status response) => 400)))) 
