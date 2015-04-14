(ns objective8.integration.db.objectives
  (:require [midje.sweet :refer :all]
            [clj-time.core :as tc]
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
                                                                           :end-date "2015-01-01T00:00:00.000Z"
                                                                           :description "description"
                                                                           :goals "goals"
                                                                           :title "title"})
                (objectives/store-objective! objective-data) =not=> (contains {:global-id anything})))))

(facts "about getting objectives"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]
        
        (fact "a stored objective can be retrieved"
              (let [{user-id :_id username :username} (sh/store-a-user)
                    objective-data {:created-by-id user-id
                                    :end-date "2015-01-01T00:00:00.000Z"
                                    :description "description"
                                    :goals "goals"
                                    :title "title"}
                    {objective-id :_id :as stored-objective} (objectives/store-objective! objective-data)]
                (objectives/retrieve-objective objective-id) => (assoc stored-objective :username username)
                (objectives/retrieve-objective objective-id) =not=> (contains {:global-id anything})))

        (fact "can retrieve a stored objective with meta information relevant to a signed in user"
              (let [objective-creator (sh/store-a-user)
                    {o-id :_id :as starred-objective} (sh/store-an-open-objective {:user objective-creator})

                    {signed-in-user-id :_id :as user} (sh/store-a-user)
                    _ (sh/store-a-star {:objective starred-objective :user user})

                    objective-uri (str "/objectives/" o-id)]
                (objectives/get-objective-as-signed-in-user o-id signed-in-user-id) => (-> starred-objective
                                                                                           (assoc :username (:username objective-creator))
                                                                                           (assoc :meta {:starred true})
                                                                                           (dissoc :global-id)
                                                                                           (assoc :uri objective-uri))))

        (fact "can retrieve a list of objectives"
              (let [{user-id :_id username :username} (sh/store-a-user)
                    objective-data {:created-by-id user-id
                                    :end-date "2015-01-01T00:00:00.000Z"
                                    :description "description"
                                    :goals "goals"
                                    :title "title"}
                    {objective-id :_id :as stored-objective} (objectives/store-objective! objective-data)]
                (objectives/retrieve-objectives) => [(assoc stored-objective :username username)]
                (first (objectives/retrieve-objectives)) =not=> (contains {:global-id anything})))

         (fact "can retrieve a list of objectives created by a given user"
               (let [{user-id :_id :as user} (sh/store-a-user)
                     {first-objective-id :_id} (sh/store-an-open-objective {:user user})
                     {second-objective-id :_id} (sh/store-an-open-objective {:user user})
                     non-owned-objective (sh/store-an-open-objective)]
                 (objectives/get-objectives-owned-by-user-id user-id) 
                 => (contains [(contains {:_id first-objective-id})
                               (contains {:_id second-objective-id})] 
                              :in-any-order)))

         (fact "can retrieve a list of starred objectives for a given user"
               (let [non-starred-objective (sh/store-an-open-objective)

                     starred-objective-for-a-different-user (sh/store-an-open-objective)
                     _ (sh/store-a-star {:objective starred-objective-for-a-different-user})

                     {username :username :as objective-creator} (sh/store-a-user)
                     {starred-objective-id :_id :as starred-objective} (sh/store-an-open-objective {:user objective-creator})
                     starred-objective-uri (str "/objectives/" starred-objective-id)
                     {user-id :created-by-id} (sh/store-a-star {:objective starred-objective})] 
                 (objectives/retrieve-starred-objectives user-id) => [(-> starred-objective
                                                                          (assoc :username username
                                                                                 :uri starred-objective-uri) 
                                                                          (dissoc :global-id))]))

         (fact "can retrieve objectives with the meta information relevant for a signed in user"
               (let [non-starred-objective (sh/store-an-open-objective)

                     starred-objective-for-a-different-user (sh/store-an-open-objective)
                     _ (sh/store-a-star {:objective starred-objective-for-a-different-user})

                     {username :username :as objective-creator} (sh/store-a-user)
                     starred-objective (sh/store-an-open-objective {:user objective-creator})

                     {user-id :created-by-id} (sh/store-a-star {:objective starred-objective})]
                 (objectives/get-objectives-as-signed-in-user user-id)
                 => (contains [(contains {:_id (:_id non-starred-objective)
                                          :meta {:starred false}}) 
                               (contains {:_id (:_id starred-objective-for-a-different-user)
                                          :meta {:starred false}}) 
                               (contains {:_id (:_id starred-objective)
                                          :meta {:starred true}})]
                              :in-any-order)))
         
         (fact "objectives due to start drafting can be retrieved"
               (let [{username :username :as user} (sh/store-a-user)
                     {o-id :_id :as past-objective} (sh/store-an-objective-due-for-drafting {:user user})
                     uri (str "/objectives/" o-id)]
                 (sh/store-an-objective-in-draft) 
                 (sh/store-an-open-objective)
                 (objectives/retrieve-objectives-due-for-drafting) => [(-> past-objective 
                                                                           (assoc :username username 
                                                                                  :uri uri)
                                                                           (dissoc :global-id))]))))
