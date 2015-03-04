(ns objective8.api.drafting-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [objective8.integration-helpers :as helpers]
            [objective8.users :as users]
            [objective8.middleware :as m]
            [objective8.objectives :as objectives]))

(def app (helpers/test-context))

(defn gen-user-with-id
  "Make a user and return the ID for use in creating other content"
  []
  (:_id (users/store-user! {:twitter-id "anything" :username "username"})))

(defn gen-objective-with-id
  "Make an objective and return the ID"
  [user-id]
  (:_id (objectives/store-objective! {:created-by-id user-id :end-date "2015-03-04"})))

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
                      (let [objective-id (gen-objective-with-id (gen-user-with-id))
                            peridot-response (p/request app (str "/dev/api/v1/objectives/" objective-id "/start-drafting")
                                                        :request-method :post 
                                                        :content-type "application/json")
                            parsed-response (helpers/peridot-response-json-body->map peridot-response)]
                        parsed-response => (contains {:drafting-started true}))))))
