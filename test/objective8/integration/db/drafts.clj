(ns objective8.integration.db.drafts
  (:require [midje.sweet :refer :all]
            [objective8.drafts :as drafts]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]))

(defn uri-for-draft [draft]
  (str "/objectives/" (:objective-id draft) "/drafts/" (:_id draft)))

(facts "about storing drafts"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

        (fact "a draft can be stored"
              (let [{objective-id :objective-id submitter-id :user-id} (sh/store-a-writer)
                    _ (sh/start-drafting! objective-id)
                    draft-data {:objective-id objective-id
                                :submitter-id submitter-id
                                :content [[:p "Some hiccup"]]}]
                (drafts/store-draft! draft-data) => (contains {:uri (contains (str "/objectives/" objective-id "/drafts/")) })
                (drafts/store-draft! draft-data) =not=> (contains {:global-id anything})))))

(facts "about retrieving drafts"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

        (fact "a draft can be retrieved by id"
              (let [objective (sh/store-an-open-objective)
                    
                    first-draft (sh/store-a-draft {:objective objective})
                    {second-draft-id :_id :as second-draft} (sh/store-a-draft {:objective objective})
                    third-draft (sh/store-a-draft {:objective objective})
                    
                    second-draft-uri (uri-for-draft second-draft)]
                
                (drafts/retrieve-draft second-draft-id) => (contains {:uri second-draft-uri
                                                                      :previous-draft-id (:_id first-draft)
                                                                      :next-draft-id (:_id third-draft)})
                (drafts/retrieve-draft second-draft-id) =not=> (contains {:global-id anything})
                (drafts/retrieve-draft second-draft-id) => (contains (-> second-draft
                                                                         (assoc :username string?)
                                                                         (dissoc :_created_at_sql_time
                                                                                 :global-id)))))

        (fact "The first 50 drafts can be retrieved for an objective in reverse chronological order"
              (let [{objective-id :_id :as objective} (sh/store-an-objective-in-draft)
                    stored-drafts (doall (->> (repeat {:objective objective})
                                              (take 51)
                                              (map sh/store-a-draft)))
                    latest-draft (last stored-drafts)
                    latest-draft-uri (uri-for-draft latest-draft)]
                (count (drafts/retrieve-drafts objective-id)) => 50
                
                (first (drafts/retrieve-drafts objective-id)) => (contains {:uri latest-draft-uri})
                (first (drafts/retrieve-drafts objective-id)) =not=> (contains {:global-id anything})
                (first (drafts/retrieve-drafts objective-id)) => (contains (-> latest-draft
                                                                               (assoc :username string?)
                                                                               (dissoc :_created_at_sql_time
                                                                                       :global-id)))))

        (fact "drafts are retrieved with a count of the number of comments"
              (let [{objective-id :_id :as objective} (sh/store-an-objective-in-draft)
                    draft (sh/store-a-draft {:objective objective})
                    _ (sh/store-a-comment {:entity draft})]
                (first (drafts/retrieve-drafts objective-id)) => (contains {:meta (contains {:comments-count 1})})))

        (fact "the latest draft can be retrieved"
              (let [{objective-id :_id :as objective} (sh/store-an-open-objective)
                    first-draft (sh/store-a-draft {:objective objective})
                    latest-draft (sh/store-a-draft {:objective objective})
                    latest-draft-uri (uri-for-draft latest-draft)]
                (drafts/retrieve-latest-draft objective-id) => (contains {:uri latest-draft-uri
                                                                          :previous-draft-id (:_id first-draft)})
                (drafts/retrieve-latest-draft objective-id) =not=> (contains {:global-id anything})
                (drafts/retrieve-latest-draft objective-id) => (contains (-> latest-draft
                                                                             (assoc :username string?)
                                                                             (dissoc :_created_at_sql_time
                                                                                     :global-id)))))))

(facts "about storing sections"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

        (fact "a section can be stored and includes the global id to pass to create-comment!"
              (let [{:keys [_id objective-id]} (sh/store-a-draft)]
                (drafts/store-section! {:section-label "12abcdef" 
                                        :draft-id _id :objective-id objective-id}) => (contains {:global-id anything})))))
