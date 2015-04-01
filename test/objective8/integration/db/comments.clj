(ns objective8.integration.db.comments
  (:require [midje.sweet :refer :all]
            [objective8.comments :as comments]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]))

(facts "about storing comments"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

        (fact "comments can be stored against a draft"
              (let [{user-id :_id :as user} (sh/store-a-user)
                    {o-id :objective-id d-id :_id :as draft} (sh/store-a-draft)
                    uri-for-draft (str "/objectives/" o-id "/drafts/" d-id)
                    comment-data {:comment-on-uri uri-for-draft
                                  :comment "A comment"
                                  :created-by-id user-id}]
                (comments/store-comment-for! draft comment-data) => (contains {:_id integer?
                                                                               :uri (contains "/comments/")
                                                                               :comment-on-uri uri-for-draft
                                                                               :comment "A comment"
                                                                               :created-by-id user-id})
                (comments/store-comment-for! draft comment-data) =not=> (contains {:comment-on-id anything})
                (comments/store-comment-for! draft comment-data) =not=> (contains {:global-id anything})))

        (fact "comments can be stored against an objective"
              (let [{user-id :_id :as user} (sh/store-a-user)
                    {o-id :_id :as objective} (sh/store-an-open-objective)
                    uri-for-objective (str "/objectives/" o-id)
                    comment-data {:comment-on-uri uri-for-objective
                                  :comment "A comment"
                                  :created-by-id user-id}]
                (comments/store-comment-for! objective comment-data) => (contains {:_id integer?
                                                                                   :comment-on-uri uri-for-objective
                                                                                   :comment "A comment"
                                                                                   :created-by-id user-id})
                (comments/store-comment-for! objective comment-data) =not=> (contains {:comment-on-id anything})))))


(facts "about getting comments by uri"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]
        (fact "gets the comments in reverse chronological order with aggregate votes"
              (let [user (sh/store-a-user)
                    {draft-id :_id objective-id :objective-id :as draft} (sh/store-a-draft)
                    draft-uri (str "/objectives/" objective-id "/drafts/" draft-id)
                    stored-comments (doall (->> (repeat {:entity draft :user user})
                                                (take 1)
                                                (map sh/store-a-comment)
                                                (map #(dissoc % :global-id :comment-on-id))
                                                (map #(assoc % :comment-on-uri draft-uri 
                                                             :uri (str "/comments/" (:_id %))))))]
                (comments/get-comments draft-uri) => (contains (map contains (reverse stored-comments)))
                (first (comments/get-comments draft-uri)) => (contains {:votes {:up 0 :down 0}})
                (first (comments/get-comments draft-uri)) =not=> (contains {:comment-on-id anything})    
                (first (comments/get-comments draft-uri)) =not=> (contains {:global-id anything})))))
