(ns objective8.integration.scheduler
  (:require [midje.sweet :refer :all]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.back-end.scheduler :as scheduler]))

(def ANY_SCHEDULED_TIME "11-11-2015")

(facts "about starting drafting on objectives"
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]
         

         (fact "objective status set to drafting"
               (let [{username :username :as user} (sh/store-a-user)
                     {o-id :_id :as objective} (sh/store-an-objective-due-for-drafting {:user user})]
                 (scheduler/update-objectives ANY_SCHEDULED_TIME) => 
                 (contains (-> objective
                               (assoc :status "drafting"
                                      :uri (str "/objectives/" o-id)
                                      :username username)))))

         (fact "active invitations status set to expired"
               (let [{objective-id :_id :as objective} (sh/store-an-objective-due-for-drafting)
                     {active-invitation-id :_id :as invitation} (sh/store-an-invitation {:objective objective})
                     {accepted-invitation-id :_id} (sh/store-an-invitation {:objective objective :status "accepted"})

                     {active-invitation-for-other-objective-id :_id :as invitation-1} (sh/store-an-invitation)]
                 (scheduler/update-objectives ANY_SCHEDULED_TIME) 

                 (:status (sh/retrieve-invitation active-invitation-id)) => "expired"
                 (:status (sh/retrieve-invitation accepted-invitation-id)) => "accepted"

                 (:status (sh/retrieve-invitation active-invitation-for-other-objective-id)) => "active"))))
