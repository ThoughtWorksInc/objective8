(ns objective8.integration.db.stars
  (:require [midje.sweet :refer :all]
            [objective8.back-end.domain.stars :as stars]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]))

(facts "about storing stars"
       (against-background
         [(before :contents (do (ih/db-connection)
                                (ih/truncate-tables)))
          (after :facts (ih/truncate-tables))]
         
         (fact "stars on an objective can be stored"
               (let [{objective-id :_id user-id :created-by-id} (sh/store-an-objective)
                     star-data {:created-by-id user-id :objective-id objective-id}]
                 (stars/store-star! star-data) => (contains {:_id integer?
                                                             :objective-id objective-id
                                                             :created-by-id user-id
                                                             :entity :star
                                                             :active true})))))

(facts "about getting stars"
       (against-background
         [(before :contents (do (ih/db-connection)
                                (ih/truncate-tables)))
          (after :facts (ih/truncate-tables))]

         (fact "can get a star by objective-uri and created-by-uri"
               (let [{objective-id :objective-id created-by-id :created-by-id :as star} (sh/store-a-star)
                     objective-uri (str "/objectives/" objective-id)
                     created-by-uri (str "/users/" created-by-id)]
                 (stars/get-star objective-uri created-by-uri) => star))))

(facts "about toggling stars"
       (against-background
         [(before :contents (do (ih/db-connection)
                                (ih/truncate-tables)))
          (after :facts (ih/truncate-tables))]

         (fact "the state of a star can be toggled"
               (let [{star-id :_id :as star} (sh/store-a-star)]
                 (stars/toggle-star! star) => (assoc star :active false)))))
