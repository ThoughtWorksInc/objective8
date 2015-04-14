(ns objective8.integration.db.marks
  (:require [midje.sweet :refer :all]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]
            [objective8.marks :as marks]))

(facts "about storing marks"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

        (facts "a mark can be stored for a question"
               (let [{objective-id :_id :as objective} (sh/store-an-open-objective)

                     {question-id :_id} (sh/store-a-question {:objective objective})
                     {user-id :user-id} (sh/store-a-candidate {:invitation (sh/store-an-invitation {:objective objective})})

                     question-uri (str "/objectives/" objective-id "/questions/" question-id)
                     created-by-uri (str "/users/" user-id)

                     mark-data {:question-uri question-uri
                                :created-by-uri created-by-uri}]
                 (marks/store-mark! mark-data) => (contains {:entity :mark
                                                             :uri (contains "/meta/marks/")
                                                             :question-uri question-uri
                                                             :created-by-uri created-by-uri
                                                             :active true
                                                             :_created_at anything})))))
