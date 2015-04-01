(ns objective8.integration.scheduler
  (:require [midje.sweet :refer :all]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.scheduler :as scheduler]))

(facts "about starting drafting on objectives"
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]
         

         (fact "objective status set to drafting"
               (let [{username :username :as user} (sh/store-a-user)
                     {o-id :_id :as objective} (sh/store-an-objective-due-for-drafting user)]
                 (scheduler/update-objectives) => (contains (-> objective
                                                                (assoc :status "drafting"
                                                                       :drafting-started true
                                                                       :uri (str "/objectives/" o-id)
                                                                       :username username)))))))
