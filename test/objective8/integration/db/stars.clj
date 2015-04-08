(ns objective8.integration.db.stars
  (:require [midje.sweet :refer :all]
            [objective8.stars :as stars]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]))

(facts "about storing stars"
       (against-background
         [(before :contents (do (ih/db-connection)
                                (ih/truncate-tables)))
          (after :facts (ih/truncate-tables))]
         
         (fact "stars on an objective can be stored"
               (let [{objective-id :_id user-id :created-by-id} (sh/store-an-open-objective)
                     star-data {:created-by-id user-id :objective-id objective-id}]
                 (stars/store-star! star-data) => (contains {:_id integer?
                                                             :objective-id objective-id
                                                             :created-by-id user-id
                                                             :entity :star
                                                             :active true})))))
(facts "about retrieving stars"
       (against-background
         [(before :contents (do (ih/db-connection)
                                (ih/truncate-tables)))
          (after :facts (ih/truncate-tables))]

         (fact "gets starred objectives for a user"
               (let [{user-id :_id :as user} (sh/store-a-user)
                     stored-star (sh/store-a-star user-id)]
                 (first (stars/retrieve-starred-objectives user-id)) => (contains {:active true})
                 (first (stars/retrieve-starred-objectives user-id)) => (contains {:title "test title"})))))
