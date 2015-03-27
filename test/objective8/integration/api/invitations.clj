(ns objective8.integration.api.invitations
  (:require [peridot.core :as p]
            [midje.sweet :refer :all]
            [cheshire.core :as json]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
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
    :writer-email "writer@email.com"
    :reason "She's cool"
    :objective-id objective-id
    :invited-by-id invited-by-id }))

(facts "POST /api/v1/objectives/:id/writer-invitations"
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

(facts "GET /api/v1/invitations?uuid=<UUID>"
       (against-background
         [(m/valid-credentials? anything anything anything) => true 
          (before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (tabular
           (fact "retrieves the invitation with the given uuid if it exists"
                 (let [{uuid :uuid :as stored-invitation} (sh/store-an-invitation {:status ?status})
                       {response :response} (p/request app (str "/api/v1/invitations?uuid=" uuid))]
                   (:body response) => (helpers/json-contains stored-invitation)))
           ?status 
           "accepted" "active" "declined" "expired")

         (fact "returns a 404 status if an invitation with uuid=<UUID> doesn't exist"
               (get-in (p/request app "/api/v1/invitations?uuid=non-existent-uuid")
                       [:response :status]) => 404)

         (fact "returns a 400 status when a PSQLException is raised"
               (against-background
                (invitations/get-invitation anything) =throws=> (org.postgresql.util.PSQLException.
                                                                  (org.postgresql.util.ServerErrorMessage. "" 0)))
               (get-in (p/request app "/api/v1/invitations?uuid=some-uuid")
                       [:response :status]) => 400)))

(facts "PUT /api/v1/objectives/:obj-id/writer-invitations/:inv-id"
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

        (fact "returns a 404 when the associated objective is in drafting"
              (let [objective (sh/store-an-objective-in-draft)
                    {i-id :_id o-id :objective-id uuid :uuid} (sh/store-an-invitation {:objective objective})
                    invitation-response {:invitation-uuid uuid}
                    {response :response} (p/request app (str "/api/v1/objectives/" o-id
                                                             "/writer-invitations/" i-id)
                                                    :request-method :put
                                                    :content-type "application/json"
                                                    :body (json/generate-string invitation-response))]
                (:status response) => 404))
               
        (fact "returns 404 when no active invitation exists with the given id"
              (let [invitation-response-as-json (json/generate-string {:invitation-uuid "nonexistent uuid"})
                    {response :response} (p/request app (str "/api/v1/objectives/" "3"
                                                             "/writer-invitations/" "10")
                                                    :request-method :put
                                                    :content-type "application/json"
                                                    :body invitation-response-as-json)]
                (:status response) => 404))))
