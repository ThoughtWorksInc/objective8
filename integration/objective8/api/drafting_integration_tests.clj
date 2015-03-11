(ns objective8.api.drafting-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.integration-helpers :as helpers]
            [objective8.storage-helpers :as sh]
            [objective8.middleware :as m]))

(def app (helpers/test-context))

(facts "POST /dev/api/v1/objectives/obj-id/start-drafting" :integration
  (against-background
      (m/valid-credentials? anything anything anything) => true)
  (against-background
    [(before :contents (do (helpers/db-connection)
                           (helpers/truncate-tables)))
     (after :facts (helpers/truncate-tables))]
    
    (fact "drafting-started flag for objective set to true" 
      (let [{objective-id :_id} (sh/store-an-objective)
            {response :response} (p/request app (str "/dev/api/v1/objectives/" objective-id "/start-drafting")
                                   :request-method :post
                                   :content-type "application/json")]
        (:body response) => (helpers/json-contains {:drafting-started true})))

    (fact "active invitations status set to expired"
      (let [{objective-id :_id :as objective} (sh/store-an-objective)
            {active-invitation-id :_id} (sh/store-an-invitation {:objective objective})
            {accepted-invitation-id :_id} (sh/store-an-invitation {:objective objective :status "accepted"})

            {active-invitation-for-other-objective-id :_id} (sh/store-an-invitation)

            {response :response} (p/request app (str "/dev/api/v1/objectives/" objective-id "/start-drafting")
                                   :request-method :post
                                   :content-type "application/json")]
        (:status (sh/retrieve-invitation active-invitation-id)) => "expired"
        (:status (sh/retrieve-invitation accepted-invitation-id)) => "accepted"

        (:status (sh/retrieve-invitation active-invitation-for-other-objective-id)) => "active"))))

(facts "POST /dev/api/v1/objectives/:id/drafts" :integration
  (against-background
    (m/valid-credentials? anything anything anything) => true)
  (against-background
    [(before :contents (do (helpers/db-connection)
                           (helpers/truncate-tables)))
     (after :facts (helpers/truncate-tables))]
    
    (fact "creates a draft"
          (let [{objective-id :objective-id submitter-id :user-id} (sh/store-a-candidate)
                the-draft {:objective-id objective-id
                           :submitter-id submitter-id
                           :content "Some content"}
                {response :response} (p/request app (utils/path-for :api/post-draft :id objective-id)
                                            :request-method :post
                                            :content-type "application/json"
                                            :body (json/generate-string the-draft))
                {draft-id :_id :as stored-draft} (sh/retrieve-latest-draft objective-id)
                target-path (utils/path-for :api/get-draft :id objective-id :d-id draft-id)]
            (:body response) => (helpers/json-contains {:_id draft-id
                                                        :objective-id objective-id
                                                        :submitter-id submitter-id
                                                        :content "Some content"})
            (:status response) => 201
            (:headers response) => (helpers/location-contains target-path)))))

(facts "GET /dev/api/v1/objectives/:id/drafts/:d-id" :integration
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "gets a draft for an objective"
              (let [{objective-id :objective-id draft-id :_id :as draft} (sh/store-a-draft)
                    {response :response} (p/request app (utils/path-for :api/get-draft :id objective-id :d-id draft-id))]
                (:status response) => 200
                (:body response) => (helpers/json-contains draft)))))
