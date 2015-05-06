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


(defn store-comment-with-votes [entities vote-counts]
  (let [comment (sh/store-a-comment entities)]
    (doseq [vote-type [:up :down]
            _ (range (get vote-counts vote-type 0))]
      (sh/store-an-up-down-vote (:global-id comment) vote-type))
    comment))

(facts "about getting comments by uri"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

        (fact "gets the comments in the requested order"
              (let [objective (sh/store-an-open-objective)
                    objective-uri (str "/objectives/" (:_id objective))

                    {first-comment-id :_id} (-> (sh/store-a-comment {:entity objective}) (sh/with-votes {:up 2 :down 1}))
                    {second-comment-id :_id} (sh/store-a-comment {:entity objective})
                    {third-comment-id :_id} (-> (sh/store-a-comment {:entity objective}) (sh/with-votes {:up 1 :down 2}))]
                (comments/get-comments objective-uri {:sorted-by :created-at :filter-type :none}) => (contains [(contains {:_id third-comment-id})
                                                                                           (contains {:_id second-comment-id})
                                                                                           (contains {:_id first-comment-id})])

                (comments/get-comments objective-uri {:sorted-by :up-votes :filter-type :none})=> (contains [(contains {:_id first-comment-id})
                                                                                         (contains {:_id third-comment-id})
                                                                                         (contains {:_id second-comment-id})])

                (comments/get-comments objective-uri {:sorted-by :down-votes :filter-type :none})=> (contains [(contains {:_id third-comment-id})
                                                                                           (contains {:_id first-comment-id})
                                                                                           (contains {:_id second-comment-id})])))


      (fact "filters comments according to filter type"
            (let [objective (sh/store-an-open-objective)
                  objective-uri (str "/objectives/" (:_id objective))
                  {comment-without-note-id :_id} (sh/store-a-comment {:entity objective :comment-text "without note"})
                  {comment-with-note-id :_id} (-> (sh/store-a-comment {:entity objective :comment-text "with note"})
                                                  sh/with-note)]
             (comments/get-comments objective-uri {:filter-type :has-writer-note}) => (just [(contains {:_id comment-with-note-id})])
             (comments/get-comments objective-uri {:filter-type :none}) => (contains [(contains {:_id comment-with-note-id})
                                                                                      (contains {:_id comment-without-note-id})]
                                                                                     :in-any-order)))

        (tabular
         (fact "gets comments with aggregate votes"
               (let [objective (sh/store-an-open-objective)
                     objective-uri (str "/objectives/" (:_id objective))

                     comment (-> (sh/store-a-comment {:entity objective}) (sh/with-votes {:up 2 :down 10}))]
                 (first (comments/get-comments objective-uri {:sorted-by ?sorted-by :filter-type :none})) => (contains {:votes {:up 2 :down 10}})))
         ?sorted-by :up-votes :down-votes :created-at)

        (tabular
         (fact "gets comments with user name"
               (let [objective (sh/store-an-open-objective)
                     objective-uri (str "/objectives/" (:_id objective))

                     user (sh/store-a-user)

                     comment (sh/store-a-comment {:user user :entity objective})]
                (first (comments/get-comments objective-uri {:sorted-by ?sorted-by :filter-type :none})) => (contains {:username (:username user)})))
         ?sorted-by :up-votes :down-votes :created-at)

        (tabular
         (fact "gets comments with uris rather than global ids"
               (let [objective (sh/store-an-open-objective)
                     objective-uri (str "/objectives/" (:_id objective))

                     comment (sh/store-a-comment {:entity objective})
                     comment-uri (str "/comments/" (:_id comment))]
                 (first (comments/get-comments objective-uri {:sorted-by ?sorted-by :filter-type :none})) =not=> (contains {:comment-on-id anything})    
                 (first (comments/get-comments objective-uri {:sorted-by ?sorted-by :filter-type :none})) =not=> (contains {:global-id anything})
                 (first (comments/get-comments objective-uri {:sorted-by ?sorted-by :filter-type :none})) => (contains {:uri comment-uri
                                                                                                    :comment-on-uri objective-uri})))
         ?sorted-by :up-votes :down-votes :created-at)))
