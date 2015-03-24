(ns objective8.integration.db.comments
  (:require [midje.sweet :refer :all]
            [objective8.comments :as comments]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]))

(facts "about storing comments"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))])

       (fact "comments are stored against a URI"
             (let [{user-id :_id :as user} (sh/store-a-user)
                    {o-id :objective-id d-id :_id global-id :global-id} (sh/store-a-draft)
                    uri-for-draft (str "/objectives/" o-id "/drafts/" d-id)
                    comment-data {:comment-on-uri uri-for-draft
                                  :comment "A comment"
                                  :created-by-id user-id}]
               (comments/store-comment! comment-data) => (contains {:_id integer?
                                                                    :comment-on-uri uri-for-draft
                                                                    :comment "A comment"
                                                                    :created-by-id user-id})
               (comments/store-comment! comment-data) =not=> (contains {:comment-on-id anything}))))
