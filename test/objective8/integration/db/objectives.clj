(ns objective8.integration.db.objectives
  (:require [midje.sweet :refer :all]
            [objective8.objectives :as objectives]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]))

(facts "about storing objectives"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

        (fact "an objective can be stored"
              (let [{user-id :_id} (sh/store-a-user)
                    objective-data {:created-by-id user-id
                                    :end-date "2015-01-01T00:00:00Z"
                                    :description "description"
                                    :goals "goals"
                                    :title "title"}]
                (objectives/store-objective! objective-data) => (contains {:_id integer?
                                                                           :uri (contains "/objectives/")
                                                                           :created-by-id user-id
                                                                           :end-date "2015-01-01T00:00:00Z"
                                                                           :description "description"
                                                                           :goals "goals"
                                                                           :title "title"})
                ;(objectives/store-objective! objective-data) =not=> (contains {:global-id anything})
                ))))

(facts "about getting objectives"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]
        
        (fact "a stored objective can be retrieved"
              (let [{user-id :_id username :username} (sh/store-a-user)
                    objective-data {:created-by-id user-id
                                    :end-date "2015-01-01T00:00:00Z"
                                    :description "description"
                                    :goals "goals"
                                    :title "title"}
                    {objective-id :_id :as stored-objective} (objectives/store-objective! objective-data)]
                (objectives/retrieve-objective objective-id) => (assoc stored-objective :username username)
                ;(objectives/retrieve-objective objective-id) =not=> (contains {:global-id anything})
                ))

        (fact "can retrieve a list of objectives"
              (let [{user-id :_id username :username} (sh/store-a-user)
                    objective-data {:created-by-id user-id
                                    :end-date "2015-01-01T00:00:00Z"
                                    :description "description"
                                    :goals "goals"
                                    :title "title"}
                    {objective-id :_id :as stored-objective} (objectives/store-objective! objective-data)]
                (objectives/retrieve-objectives) => [(assoc stored-objective :username username)]
                ;(objectives/retrieve-objective objective-id) =not=> (contains {:global-id anything})
                ))))
