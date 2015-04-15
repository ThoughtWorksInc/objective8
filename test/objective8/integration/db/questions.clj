(ns objective8.integration.db.questions
  (:require [midje.sweet :refer :all]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]
            [objective8.questions :as questions]))

(facts "about getting questions"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

        (fact "can get a stored question"
              (let [{objective-id :_id} (sh/store-an-open-objective)
                    {created-by-id :_id creator-name :username :as question-creator} (sh/store-a-user)
                    question-data {:created-by-id created-by-id
                                   :objective-id objective-id
                                   :question "A question"}
                    stored-question (questions/store-question! question-data)]
                (questions/retrieve-question (:_id stored-question)) => {:entity :question
                                                                         :_created_at (:_created_at stored-question)
                                                                         :_id (:_id stored-question)
                                                                         :question "A question"
                                                                         :username creator-name
                                                                         :objective-id objective-id
                                                                         :created-by-id created-by-id
                                                                         :meta {:marked false}}))

        (fact "can retrieve mark information for a marked question"
              (let [mark (sh/store-a-mark)]
                (questions/retrieve-question (:question-id mark)) => (contains {:meta (contains {:marked true
                                                                                                 :marked-by string?})})))

        (fact "only the most recent mark information is included"
              (let [question (sh/store-a-question)
                    old-mark (sh/store-a-mark {:question question})
                    new-mark (sh/store-a-mark {:question question :active false})]
                (questions/retrieve-question (:_id question)) => (contains {:meta {:marked false}})))

        (fact "can get questions for an objective along with marking information"
              (let [{objective-id :_id :as objective} (sh/store-an-open-objective)

                    marked-question (sh/store-a-question {:objective objective})
                    _ (sh/store-a-mark {:question marked-question})

                    unmarked-question (sh/store-a-question {:objective objective})]
                (questions/retrieve-questions objective-id) => (contains [(contains {:_id (:_id marked-question)
                                                                                     :meta (contains {:marked true
                                                                                                      :marked-by string?})})
                                                                          (contains {:_id (:_id unmarked-question)
                                                                                     :meta (contains {:marked false})})]
                                                                         :in-any-order)))))
