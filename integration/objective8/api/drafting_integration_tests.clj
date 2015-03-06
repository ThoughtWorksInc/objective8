(ns objective8.api.drafting-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [objective8.integration-helpers :as helpers]
            [objective8.storage-helpers :as sh]
            [objective8.middleware :as m]))

(def app (helpers/test-context))

(facts "drafting" :integration
       (against-background
          [(before :contents (do (helpers/db-connection)
                                 (helpers/truncate-tables)))
           (after :facts (helpers/truncate-tables))]

         (facts "POST /dev/api/v1/objectives/obj-id/start-drafting"
                (against-background
                  (m/valid-credentials? anything anything anything) => true)
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

                        (:status (sh/retrieve-invitation active-invitation-for-other-objective-id)) => "active")))))
