(ns objective8.integration.api.writers
  (:require [midje.sweet :refer :all] 
            [peridot.core :as p] 
            [cheshire.core :as json]
            [objective8.integration.integration-helpers :as helpers] 
            [objective8.integration.storage-helpers :as sh]
            [objective8.core :as core]
            [objective8.middleware :as m]
            [objective8.back-end.writers :as writers]))

(def app (helpers/test-context))

(facts "POST /api/v1/objectives/:obj-id/writers"
     (against-background
       [(m/valid-credentials? anything anything anything) => true
          (before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

       (fact "creates a writer and accepts the invitation"
             (let [{invitation-id :_id
                    objective-id :objective-id
                    invitation-reason :reason
                    writer-name :writer-name
                    invitation-uuid :uuid} (sh/store-an-invitation)

                    {invitee-id :_id} (sh/store-a-user)
                    writer-data {:invitation-uuid invitation-uuid
                                 :invitee-id invitee-id
                                 :objective-id objective-id}
                    {response :response} (p/request app (str "/api/v1/objectives/" objective-id
                                                             "/writers")
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string writer-data))
                    updated-invitation (sh/retrieve-invitation invitation-id)]
               (:status updated-invitation) => "accepted"
               (:status response) => 201
               (:headers response) => (helpers/location-contains (str "/api/v1/objectives/" objective-id
                                                                      "/writers/"))
               (:body response) => (helpers/json-contains {:_id integer?
                                                           :user-id invitee-id
                                                           :invitation-id invitation-id
                                                           :objective-id objective-id
                                                           :invitation-reason invitation-reason
                                                           :writer-name writer-name})))

       (fact "returns 403 status when no active invitation exists with given uuid"
             (let [{invitee-id :_id} (sh/store-a-user)
                   {objective-id :_id} (sh/store-an-open-objective)
                   writer-data {:invitation-uuid "nonexistent uuid"
                                :invitee-id invitee-id
                                :objective-id objective-id
                                :invitation-reason "some reason"
                                :writer-name "writer name"}
                   {response :response} (p/request app (str "/api/v1/objectives/" objective-id
                                                            "/writers")
                                                   :request-method :post
                                                   :content-type "application/json"
                                                   :body (json/generate-string writer-data))]
               (:status response) => 403))

       (fact "returns a 403 status when drafting has started on the objective"
             (let [{invitee-id :_id} (sh/store-a-user)
                   {o-id :_id :as objective} (sh/store-an-objective-in-draft)
                   {i-id :_id uuid :uuid} (sh/store-an-invitation {:objective objective})
                   writer-data {:invitation-uuid uuid
                                   :invitee-id invitee-id
                                   :objective-id o-id
                                   :invitation-reason "some reason"
                                   :writer-name "writer name"}
                   {response :response} (p/request app (str "/api/v1/objectives/" o-id "/writers")
                                                   :request-method :post
                                                   :content-type "application/json"
                                                   :body (json/generate-string writer-data))]
               (:status response) => 403))))

(facts "GET /api/v1/objectives/:id/writers"
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "retrieves the writers for an objective"
              (let [{o-id :objective-id :as invitation} (sh/store-an-invitation)
                    writers (doall (->> (repeat {:invitation invitation})
                                           (take 5)
                                           (map sh/store-a-writer)))
                    {response :response} (p/request app (str "/api/v1/objectives/" o-id "/writers"))]
                (:body response) => (helpers/json-contains (map contains writers))))))


(facts "GET /api/v1/writers/:id/objectives"
       (against-background
         [(m/valid-credentials? anything anything anything) => true
          (before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))])
       
       (fact "retrieves list of objectives that belongs to the writer"
             (let [{user-id :_id :as user} (sh/store-a-user)
                   objective (sh/store-an-open-objective)
                   second-objective (sh/store-an-open-objective)
                   objective-for-another-user (sh/store-an-open-objective)
                   _ (sh/store-a-writer {:objective objective :user user})
                   _ (sh/store-a-writer {:objective second-objective :user user})
                   _ (sh/store-a-writer {:objective objective-for-another-user})
                   {response :response} (p/request app (str "/api/v1/writers/" user-id "/objectives"))]
             (:body response) => (helpers/json-contains 
                                   (map contains (->> [second-objective objective] 
                                                          (map #(dissoc % :global-id)))))
             (:body response) =not=> (helpers/json-contains objective-for-another-user))))

