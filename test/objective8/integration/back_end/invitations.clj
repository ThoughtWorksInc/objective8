(ns objective8.integration.back-end.invitations
  (:require [peridot.core :as p]
            [midje.sweet :refer :all]
            [cheshire.core :as json]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.back-end.domain.writers :as writers]
            [objective8.back-end.domain.invitations :as invitations]
            [objective8.back-end.domain.users :as users]
            [objective8.back-end.domain.objectives :as objectives]
            [objective8.middleware :as m])) 

(def app (helpers/api-context))
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
    :invited-by-id invited-by-id}))

(facts "POST /api/v1/objectives/:id/writer-invitations"
       (against-background
         (m/valid-credentials? anything anything anything) => true)
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "the invitation is stored when the inviter is authorised"
               (let [{obj-id :_id created-by-id :created-by-id} (sh/store-an-objective)
                     invitation (an-invitation obj-id created-by-id)
                     {response :response} (p/request app (utils/api-path-for :api/post-invitation :id obj-id)
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body (json/generate-string invitation))]
                 (:status response) => 201
                 (:body response) => (helpers/json-contains (assoc invitation :_id integer?))
                 (:headers response) => (helpers/location-contains (str "/api/v1/objectives/" obj-id "/writer-invitations/"))))

         (fact "a 400 status is returned if a PSQLException is raised"
               (against-background
                 (invitations/store-invitation! anything) =throws=> (org.postgresql.util.PSQLException.
                                                                      (org.postgresql.util.ServerErrorMessage. "" 0)))
               (let [{obj-id :_id user-id :created-by-id} (sh/store-an-objective)]
                 (get-in (p/request app (utils/api-path-for :api/post-invitation :id obj-id)
                                    :request-method :post
                                    :content-type "application/json"
                                    :body (json/generate-string (an-invitation obj-id user-id)))
                         [:response :status])) => 400))) 

(facts "GET /api/v1/invitations?uuid=<UUID>"
       (against-background
         [(m/valid-credentials? anything anything anything) => true 
          (before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (tabular
           (fact "retrieves the invitation with the given uuid if it exists"
                 (let [{uuid :uuid :as stored-invitation} (sh/store-an-invitation {:status ?status})
                       {response :response} (p/request app (str (utils/api-path-for :api/get-invitation) "?uuid=" uuid))]
                   (:body response) => (helpers/json-contains stored-invitation)))
           ?status 
           "accepted" "active" "declined" "expired")

         (fact "returns a 404 status if an invitation with uuid=<UUID> doesn't exist"
               (get-in (p/request app (str (utils/api-path-for :api/get-invitation) "?uuid=non-existent-uuid"))
                       [:response :status]) => 404)

         (fact "returns a 400 status when a PSQLException is raised"
               (against-background
                 (invitations/get-invitation anything) =throws=> (org.postgresql.util.PSQLException.
                                                                   (org.postgresql.util.ServerErrorMessage. "" 0)))
               (get-in (p/request app (str (utils/api-path-for :api/get-invitation) "?uuid=some-uuid"))
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
                     {response :response} (p/request app (utils/api-path-for :api/put-invitation-declination :id objective-id :i-id invitation-id)
                                                     :request-method :put
                                                     :content-type "application/json"
                                                     :body (json/generate-string invitation-response))
                     updated-invitation (sh/retrieve-invitation invitation-id)]
                 (:status response) => 200
                 (:body response) => (helpers/json-contains updated-invitation)
                 (:status updated-invitation) => "declined"))

         (fact "returns 404 when no active invitation exists with the given id"
               (let [invitation-response-as-json (json/generate-string {:invitation-uuid "nonexistent uuid"})
                     {response :response} (p/request app (utils/api-path-for :api/put-invitation-declination :id 3 :i-id 10)
                                                     :request-method :put
                                                     :content-type "application/json"
                                                     :body invitation-response-as-json)]
                 (:status response) => 404)))) 
