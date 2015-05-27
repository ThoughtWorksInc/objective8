(ns objective8.integration.db.marks
  (:require [midje.sweet :refer :all]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]
            [objective8.back-end.domain.marks :as marks]))

(facts "about storing marks"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

        (facts "a mark can be stored for a question"
               (let [{objective-id :_id :as objective} (sh/store-an-objective)

                     {question-id :_id} (sh/store-a-question {:objective objective})
                     {user-id :user-id} (sh/store-a-writer {:invitation (sh/store-an-invitation {:objective objective})})

                     question-uri (str "/objectives/" objective-id "/questions/" question-id)
                     created-by-uri (str "/users/" user-id)

                     mark-data {:question-uri question-uri
                                :created-by-uri created-by-uri
                                :active true}]
                 (marks/store-mark! mark-data) => (contains {:entity :mark
                                                             :uri (contains "/meta/marks/")
                                                             :question-uri question-uri
                                                             :created-by-uri created-by-uri
                                                             :active true
                                                             :_created_at anything})))))

(facts "about getting marks for questions"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

        (fact "the most recently created mark is returned"
              (let [{objective-id :objective-id question-id :_id :as question} (sh/store-a-question)

                    mark (sh/store-a-mark {:question question})
                    new-mark (sh/store-a-mark {:question question :active false})

                    question-uri (str "/objectives/" objective-id "/questions/" question-id)

                    created-by-uri (str "/users/" (:created-by-id new-mark))
                    new-mark-uri (str "/meta/marks/" (:_id new-mark))]
                (marks/get-mark-for-question question-uri) => {:uri new-mark-uri
                                                               :question-uri question-uri
                                                               :created-by-uri created-by-uri
                                                               :active false
                                                               :_created_at (:_created_at new-mark)
                                                               :entity :mark}))))
