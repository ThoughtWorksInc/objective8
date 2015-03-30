(ns objective8.integration.db.answers
  (:require [midje.sweet :refer :all]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]
            [objective8.answers :as answers]))

(background
 [(before :contents (do (ih/db-connection)
                        (ih/truncate-tables)))
  (after :facts (ih/truncate-tables))])


(facts "about storing answers"
       (fact "an answer can be stored"
             (let [{user-id :_id} (sh/store-a-user)
                   {q-id :_id o-id :objective-id} (sh/store-a-question)
                   answer-data {:created-by-id user-id
                                :objective-id o-id
                                :question-id q-id
                                :answer "An answer"}
                   answer-uri (str "/objectives/" o-id "/questions/" q-id "/answers/")]
               (answers/store-answer! answer-data) => (contains {:entity :answer
                                                                 :_id integer?
                                                                 :objective-id o-id
                                                                 :question-id q-id
                                                                 :created-by-id user-id
                                                                 :answer "An answer"
                                                                 :uri (contains answer-uri)})
               (answers/store-answer! answer-data) =not=> (contains {:global-id anything}))))

(facts "about retrieving answers"
       (fact "answers for a question can be retrieved"
             (let [{o-id :objective-id q-id :question-id a-id :_id :as stored-answer} (sh/store-an-answer)
                   answer-uri (str "/objectives/" o-id "/questions/" q-id "/answers/" a-id)]
               (answers/get-answers q-id) => (contains [(contains (-> stored-answer
                                                                      (assoc :uri answer-uri)
                                                                      (dissoc :global-id)))])
               (answers/get-answers q-id) =not=> (contains [(contains {:global-id anything})]))))
