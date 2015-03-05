(ns objective8.api.drafting-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [objective8.integration-helpers :as helpers]
            [objective8.storage-helpers :as sh]
            [objective8.users :as users]
            [objective8.middleware :as m]
            [objective8.objectives :as objectives]))

(def app (helpers/test-context))

(facts "drafting" :integration
       (against-background
         [(before :contents (do
                              (helpers/db-connection)
                              (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (facts "POST /dev/api/v1/objectives/obj-id/start-drafting"
                (against-background
                  (m/valid-credentials? anything anything anything) => true)
                (fact "drafting-started flag for objective set to true" 
                      (let [objective-id (:_id (sh/store-an-objective))
                            peridot-response (p/request app (str "/dev/api/v1/objectives/" objective-id "/start-drafting")
                                                        :request-method :post 
                                                        :content-type "application/json")
                            parsed-response (helpers/peridot-response-json-body->map peridot-response)]
                        parsed-response => (contains {:drafting-started true}))))))
