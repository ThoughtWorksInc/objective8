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
