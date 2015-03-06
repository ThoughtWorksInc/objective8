(ns objective8.api.invitations-integration-tests
  (:require [peridot.core :as p]
            [midje.sweet :refer :all]
            [cheshire.core :as json]
            [objective8.integration-helpers :as helpers]
            [objective8.storage-helpers :as sh]
            [objective8.writers :as writers]
            [objective8.invitations :as invitations]
            [objective8.users :as users]
            [objective8.objectives :as objectives]
            [objective8.middleware :as m])) 

(def app (helpers/test-context))
(def OBJECTIVE_ID 1)
(def INVITED_BY_ID 2)
(def INVITATION_ID 3)

(defn an-invitation
  ([]
   (an-invitation OBJECTIVE_ID INVITED_BY_ID))

  ([objective-id invited-by-id]
   {:writer-name "Mel"
    :reason "She's cool"
    :objective-id objective-id
    :invited-by-id invited-by-id }))

(facts "POST /api/v1/objectives/:id/writer-invitations" :integration
       (against-background
        (m/valid-credentials? anything anything anything) => true)
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "the invitation is stored"
              (let [{obj-id :_id created-by-id :created-by-id} (sh/store-an-objective)
                    invitation (an-invitation obj-id created-by-id)
                    {response :response} (p/request app (str "/api/v1/objectives/" obj-id "/writer-invitations")
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string invitation))]
                (:status response) => 201
                (:body response) => (helpers/json-contains (assoc invitation :_id integer?))
                (:headers response) => (helpers/location-contains (str "/api/v1/objectives/" obj-id "/writer-invitations/"))))

        (fact "a 423 (resource locked) status is returned when drafting has started on the objective"
              (let [{obj-id :_id created-by-id :created-by-id} (sh/store-an-objective-in-draft)
                    invitation (an-invitation obj-id created-by-id)
                    {response :response} (p/request app (str "/api/v1/objectives/" obj-id "/writer-invitations")
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string invitation))]
                (:status response) => 423))
        
        (fact "a 400 status is returned if a PSQLException is raised"
              (against-background
               (invitations/store-invitation! anything) =throws=> (org.postgresql.util.PSQLException.
                                                                   (org.postgresql.util.ServerErrorMessage. "" 0)))
              (get-in (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/writer-invitations")
                                 :request-method :post
                                 :content-type "application/json"
                                 :body (json/generate-string (an-invitation)))
                      [:response :status]) => 400)))

(facts "GET /api/v1/invitations?uuid=<UUID>" :integration
       (against-background
         [(m/valid-credentials? anything anything anything) => true 
          (before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "retrieves the active invitation with the given uuid if it exists"
               (let [{uuid :uuid :as stored-invitation} (sh/store-an-invitation)
                     {response :response} (p/request app (str "/api/v1/invitations?uuid=" uuid))]
                 (:body response) => (helpers/json-contains stored-invitation)))

         (fact "returns a 404 status if an invitation with uuid=<UUID> doesn't exist"
               (get-in (p/request app "/api/v1/invitations?uuid=non-existent-uuid")
                       [:response :status]) => 404)

         (fact "returns a 400 status when a PSQLException is raised"
               (against-background
                (invitations/get-active-invitation anything) =throws=> (org.postgresql.util.PSQLException.
                                                                        (org.postgresql.util.ServerErrorMessage. "" 0)))
               (get-in (p/request app "/api/v1/invitations?uuid=some-uuid")
                       [:response :status]) => 400)))

(facts "POST /api/v1/objectives/:obj-id/candidate-writers" :integration
     (against-background
       [(m/valid-credentials? anything anything anything) => true 
          (before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

       (fact "creates a candidate writer and accepts the invitation"
             (let [{invitation-id :_id
                    objective-id :objective-id
                    invitation-reason :reason
                    writer-name :writer-name
                    invitation-uuid :uuid} (sh/store-an-invitation)

                    {invitee-id :_id} (sh/store-a-user)
                    candidate-data {:invitation-uuid invitation-uuid
                                    :invitee-id invitee-id
                                    :objective-id objective-id}
                    {response :response} (p/request app (str "/api/v1/objectives/" objective-id
                                                             "/candidate-writers")
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string candidate-data))
                    updated-invitation (sh/retrieve-invitation invitation-id)]
               (:status updated-invitation) => "accepted"
               (:status response) => 201
               (:headers response) => (helpers/location-contains (str "/api/v1/objectives/" objective-id
                                                                      "/candidate-writers/"))
               (:body response) => (helpers/json-contains {:_id integer?
                                                           :user-id invitee-id
                                                           :invitation-id invitation-id
                                                           :objective-id objective-id
                                                           :invitation-reason invitation-reason
                                                           :writer-name writer-name})))

       (fact "returns 403 status when no invitation exists with given uuid"
             (let [{invitee-id :_id} (sh/store-a-user)
                   {objective-id :_id} (sh/store-an-objective)
                   candidate-data {:invitation-uuid "nonexistent uuid"
                                   :invitee-id invitee-id
                                   :objective-id objective-id
                                   :invitation-reason "some reason"
                                   :writer-name "writer name"}
                   {response :response} (p/request app (str "/api/v1/objectives/" objective-id
                                                            "/candidate-writers")
                                                   :request-method :post
                                                   :content-type "application/json"
                                                   :body (json/generate-string candidate-data))]
               (:status response) => 403))))

(facts "PUT /api/v1/objectives/:obj-id/writer-invitations/:inv-id" :integration
       (against-background
        [(m/valid-credentials? anything anything anything) => true 
         (before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "declines the invitation"
              (let [{invitation-id :_id objective-id :objective-id invitation-uuid :uuid} (sh/store-an-invitation)
                    invitation-response {:invitation-uuid invitation-uuid}
                    {response :response} (p/request app (str "/api/v1/objectives/" objective-id
                                                             "/writer-invitations/" invitation-id)
                                                    :request-method :put
                                                    :content-type "application/json"
                                                    :body (json/generate-string invitation-response))
                    updated-invitation (sh/retrieve-invitation invitation-id)]
                (:status response) => 200
                (:body response) => (helpers/json-contains updated-invitation)
                (:status updated-invitation) => "declined"))
               
        (fact "returns 404 when no active invitation exists with the given id"
              (let [invitation-response-as-json (json/generate-string {:invitation-uuid "nonexistent uuid"})
                    {response :response} (p/request app (str "/api/v1/objectives/" "3"
                                                             "/writer-invitations/" "10")
                                                    :request-method :put
                                                    :content-type "application/json"
                                                    :body invitation-response-as-json)]
                (:status response) => 404))))
