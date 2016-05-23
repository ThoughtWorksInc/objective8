(ns objective8.integration.db.objectives
  (:require [midje.sweet :refer :all]
            [clj-time.core :as tc]
            [objective8.back-end.domain.objectives :as objectives]
            [objective8.config :as config]
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
                                    :description "description"
                                    :title "title"}]
                (objectives/store-objective! objective-data) => (contains {:_id integer?
                                                                           :uri (contains "/objectives/")
                                                                           :created-by-id user-id
                                                                           :description "description"
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
                                    :description "description"
                                    :title "title"}
                    {objective-id :_id :as stored-objective} (objectives/store-objective! objective-data)]
                (objectives/get-objective objective-id) => (assoc stored-objective :username username)
                (objectives/get-objective objective-id) =not=> (contains {:global-id anything})))

        (fact "the objective is retrieved along with the number of times it has been starred"
              (let [{objective-id :_id :as objective} (sh/store-an-objective)
                    _ (sh/store-a-star {:objective objective}) 
                    _ (sh/store-a-star {:objective objective})]
                (objectives/get-objective objective-id) => (contains {:meta (contains {:stars-count 2})})))

        (fact "the objective is retrieved along with the number of comments posted against it"
              (let [{objective-id :_id :as objective} (sh/store-an-objective)
                    _ (sh/store-a-comment {:entity objective})
                    _ (sh/store-a-comment {:entity objective})]
                (objectives/get-objective objective-id) => (contains {:meta (contains {:comments-count 2})})))
        
        (fact "can retrieve a stored objective with meta information relevant to a signed in user"
              (let [objective-creator (sh/store-a-user)
                    {o-id :_id :as starred-objective} (sh/store-an-objective {:user objective-creator})

                    {signed-in-user-id :_id :as user} (sh/store-a-user)
                    _ (sh/store-a-star {:objective starred-objective :user user})

                    objective-uri (str "/objectives/" o-id)]
                (objectives/get-objective-as-signed-in-user o-id signed-in-user-id) 
                => (-> starred-objective
                       (assoc :username (:username objective-creator))
                       (assoc :meta {:starred true
                                     :stars-count 1
                                     :comments-count 0
                                     :drafts-count 0})
                       (dissoc :global-id)
                       (assoc :uri objective-uri))))

         (fact "the objective can be retrieved with draft count"
               (let [{user-id :_id username :username} (sh/store-a-user)
                     objective-data {:created-by-id user-id
                                     :description "description"
                                     :title "title"}
                     {objective-id :_id :as stored-objective} (objectives/store-objective! objective-data)]
                 (sh/store-a-draft {:objective stored-objective})
                 (objectives/get-objective objective-id) => (-> stored-objective
                                                                (assoc :username username)
                                                                (assoc-in [:meta :drafts-count] 1)) 
                 (objectives/get-objective objective-id) =not=> (contains {:global-id anything})))

        (fact "can retrieve a list of objectives"
              (let [{user-id :_id username :username} (sh/store-a-user)
                    objective-data {:created-by-id user-id
                                    :description "description"
                                    :title "title"}
                    {objective-id :_id :as stored-objective} (objectives/store-objective! objective-data)]
                (objectives/retrieve-objectives) => [(assoc stored-objective :username username)]
                (first (objectives/retrieve-objectives)) =not=> (contains {:global-id anything})))
         
         (fact "the retrieved list of objectives does not include removed objectives"
               (let [{username :username :as user} (sh/store-a-user) 
                     stored-objective  (sh/store-an-objective {:user user})
                     expected-retrieved-objective (-> stored-objective
                                                      (assoc :username username)
                                                      (assoc :uri (str "/objectives/" (:_id stored-objective)))
                                                      (dissoc :global-id))]
                 (sh/store-an-admin-removed-objective) 
                 (objectives/retrieve-objectives) => [expected-retrieved-objective]))

         (fact "can retrieve objectives with removed objectives if specified"
               (let [{username :username :as user} (sh/store-a-user) 
                     stored-objective  (sh/store-an-objective {:user user})
                     removed-objective (sh/store-an-admin-removed-objective {:user user}) 
                     expected-retrieved-objective (-> stored-objective
                                                      (assoc :username username)
                                                      (assoc :uri (str "/objectives/" (:_id stored-objective)))
                                                      (dissoc :global-id))
                     expected-removed-objective (-> removed-objective
                                                    (assoc :username username)
                                                    (assoc :uri (str "/objectives/" (:_id removed-objective)))
                                                    (dissoc :global-id))]
                 (objectives/retrieve-objectives true) => (just [expected-removed-objective 
                                                            expected-retrieved-objective] :in-any-order)))

         (fact "can retrieve a list of objectives created by a given user"
               (let [{user-id :_id :as user} (sh/store-a-user)
                     {first-objective-id :_id} (sh/store-an-objective {:user user})
                     {second-objective-id :_id} (sh/store-an-objective {:user user})
                     non-owned-objective (sh/store-an-objective)]
                 (objectives/get-objectives-owned-by-user-id user-id) 
                 => (contains [(contains {:_id first-objective-id})
                               (contains {:_id second-objective-id})] 
                              :in-any-order)))

         (fact "can retrieve a list of writer objectives for a given user"
               (let [{user-id :_id username :username :as user} (sh/store-a-user)
                     stored-objective (sh/store-an-objective {:user user})
                     retrieved-objective (-> stored-objective
                                             (assoc :username username
                                                    :uri (str "/objectives/" (:_id stored-objective)))
                                             (dissoc :global-id))
                     second-stored-objective (sh/store-an-objective {:user user})
                     second-retrieved-objective (-> second-stored-objective
                                                    (assoc :username username
                                                           :uri (str "/objectives/" (:_id second-stored-objective)))
                                                    (dissoc :global-id))
                     objective-for-another-user (sh/store-an-objective)]

                 (sh/store-a-writer {:objective stored-objective :user user})
                 (sh/store-a-writer {:objective second-stored-objective :user user})
                 (sh/store-a-writer {:objective objective-for-another-user})

                 (objectives/get-objectives-for-writer user-id) => (just [retrieved-objective
                                                                          second-retrieved-objective]
                                                                         :in-any-order)))

         (fact "can retrieve a list of starred objectives for a given user"
               (let [non-starred-objective (sh/store-an-objective)

                     starred-objective-for-a-different-user (sh/store-an-objective)
                     _ (sh/store-a-star {:objective starred-objective-for-a-different-user})

                     {username :username :as objective-creator} (sh/store-a-user)
                     {starred-objective-id :_id :as starred-objective} (sh/store-an-objective {:user objective-creator})
                     starred-objective-uri (str "/objectives/" starred-objective-id)
                     {user-id :created-by-id} (sh/store-a-star {:objective starred-objective})]
                 (objectives/retrieve-starred-objectives user-id) => [(-> starred-objective
                                                                          (assoc :username username
                                                                                 :uri starred-objective-uri)
                                                                          (dissoc :global-id))]))

         (fact "can retrieve objectives with the meta information relevant for a signed in user"
               (let [non-starred-objective (sh/store-an-objective)

                     starred-objective-for-a-different-user (sh/store-an-objective)
                     _ (sh/store-a-star {:objective starred-objective-for-a-different-user})

                     {username :username :as objective-creator} (sh/store-a-user)
                     starred-objective (sh/store-an-objective {:user objective-creator})

                     {user-id :created-by-id} (sh/store-a-star {:objective starred-objective})]
                 (objectives/get-objectives-as-signed-in-user user-id)
                 => (contains [(contains {:_id (:_id non-starred-objective)
                                          :meta (contains {:starred false})}) 
                               (contains {:_id (:_id starred-objective-for-a-different-user)
                                          :meta (contains {:starred false})}) 
                               (contains {:_id (:_id starred-objective)
                                          :meta (contains {:starred true})})]
                              :in-any-order)))))

(facts "about removed-by-admin status"
       (fact "can set objective 'removed-by-admin' to true"
             (let [{o-id :_id :as objective} (sh/store-an-objective)
                   uri (str "/objectives/" o-id)]
               (:removed-by-admin objective) => false
               (objectives/admin-remove-objective! objective) => (-> (dissoc objective :global-id) 
                                                                     (assoc :removed-by-admin true
                                                                            :uri uri))))) 

(facts "about promoting objectives"
       (against-background
         [(before :contents (do (ih/db-connection)
                                (ih/truncate-tables)))
          (after :facts (ih/truncate-tables))]

         (fact "Promote objective sets status of objective to promoted"
               (let [objective (sh/store-an-objective)]
                 (boolean (:promoted objective)) => false
                 (objectives/toggle-promoted-status! objective) => (-> (dissoc objective :global-id)
                                                                       (assoc :promoted true))))

         (fact "Toggle objective promotion status sets status of objective to un-promoted if objective is already promoted"
               (let [objective (sh/store-an-objective)
                     promoted-objective (-> (objectives/toggle-promoted-status! objective)
                                            (assoc :global-id (:global-id objective)))]
                 (boolean (:promoted promoted-objective)) => true
                 (objectives/toggle-promoted-status! promoted-objective) => (-> (dissoc objective :global-id)
                                                                                (assoc :promoted false))))

         (fact "Get promoted objectives only retrieves promoted objectives"
               (let [{username :username :as user} (sh/store-a-user)
                     stored-promoted-objective (sh/store-a-promoted-objective {:user user})
                     expected-retrieved-objective (-> stored-promoted-objective
                                                      (assoc :username username)
                                                      (dissoc :global-id))]
                 (sh/store-an-objective {:user user})
                 (sh/store-an-admin-removed-objective)
                 (objectives/get-promoted-objectives) => [expected-retrieved-objective])
               )))