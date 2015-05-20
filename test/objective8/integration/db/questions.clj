(ns objective8.integration.db.questions
  (:require [midje.sweet :refer :all]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]
            [objective8.back-end.domain.questions :as questions]))

(facts "about getting questions"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

        (fact "can get a stored question by its uri"
              (let [{objective-id :_id} (sh/store-an-open-objective)
                    {created-by-id :_id creator-name :username :as question-creator} (sh/store-a-user)
                    question-data {:created-by-id created-by-id
                                   :objective-id objective-id
                                   :question "A question"}
                    stored-question (questions/store-question! question-data)
                    question-uri (str "/objectives/" objective-id "/questions/" (:_id stored-question))]
                (questions/get-question question-uri) => {:entity :question
                                                          :_created_at (:_created_at stored-question)
                                                          :_id (:_id stored-question)
                                                          :question "A question"
                                                          :username creator-name
                                                          :answer-count 0
                                                          :objective-id objective-id
                                                          :created-by-id created-by-id
                                                          :uri question-uri
                                                          :meta {:marked false}}))

        (fact "attempting to get a question by a bad uri gives no result"
              (let [{objective-id :_id} (sh/store-an-open-objective)
                    {created-by-id :_id creator-name :username :as question-creator} (sh/store-a-user)
                    question-data {:created-by-id created-by-id
                                   :objective-id objective-id
                                   :question "A question"}
                    stored-question (questions/store-question! question-data)
                    bad-question-uri (str "/objectives/" (inc objective-id) "/questions/" (:_id stored-question))]
                (questions/get-question bad-question-uri) => nil))

        (fact "questions can be sorted by numbers of answers"
              (let [{objective-id :_id :as objective} (sh/store-an-open-objective)

                    {three-answer-question-id  :_id :as question-with-three-answers} (sh/store-a-question {:objective objective})
                    {one-answer-question-id  :_id :as question-with-one-answer} (sh/store-a-question {:objective objective})
                    {two-answer-question-id :_id :as question-with-two-answers} (sh/store-a-question {:objective objective})

                    objective-uri (str "/objectives/" objective-id)]

                    (sh/store-an-answer {:question question-with-three-answers})
                    (sh/store-an-answer {:question question-with-three-answers})
                    (sh/store-an-answer {:question question-with-three-answers})

                    (sh/store-an-answer {:question question-with-one-answer})

                    (sh/store-an-answer {:question question-with-two-answers})
                    (sh/store-an-answer {:question question-with-two-answers})

                (questions/get-questions-for-objective-by-most-answered objective-uri)
                => (contains [(contains {:_id three-answer-question-id})
                              (contains {:_id two-answer-question-id})
                              (contains {:_id one-answer-question-id})])))

        (fact "mark information is included when getting a question"
              (let [{objective-id :objective-id question-id :_id :as question} (sh/store-a-question)
                    mark (sh/store-a-mark {:question question})
                    question-uri (str "/objectives/" objective-id "/questions/" question-id)]
                (questions/get-question question-uri) => (contains {:meta (contains {:marked true
                                                                                     :marked-by string?})})))

        (fact "only the most recent mark information is included"
              (let [{objective-id :objective-id question-id :_id :as question} (sh/store-a-question)
                    old-mark (sh/store-a-mark {:question question})
                    new-mark (sh/store-a-mark {:question question :active false})
                    question-uri (str "/objectives/" objective-id "/questions/" question-id)]
                (questions/get-question question-uri) => (contains {:meta {:marked false}})))

        (fact "can get questions for an objective along with marking information"
              (let [{objective-id :_id :as objective} (sh/store-an-open-objective)
                    
                    marked-question (sh/store-a-question {:objective objective})
                    _ (sh/store-a-mark {:question marked-question})
                    
                    unmarked-question (sh/store-a-question {:objective objective})

                    objective-uri (str "/objectives/" objective-id)]
                (questions/get-questions-for-objective objective-uri)
                => (contains [(contains {:_id (:_id marked-question)
                                         :meta (contains {:marked true
                                                          :marked-by string?})})
                              (contains {:_id (:_id unmarked-question)
                                         :meta (contains {:marked false})})]
                             :in-any-order))))
       
       (fact "can get quesiton by objective uri with the total number of answers"
             (let [{objective-id :objective-id question-id :_id :as question} (sh/store-a-question)
                   question-uri (str "/objectives/" objective-id "/questions/" question-id)]
               (sh/store-an-answer {:question question})
               (sh/store-an-answer {:question question})
               
               (questions/get-question question-uri) => (contains {:answer-count 2}))))
