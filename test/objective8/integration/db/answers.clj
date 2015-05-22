(ns objective8.integration.db.answers
  (:require [midje.sweet :refer :all]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]
            [objective8.back-end.domain.answers :as answers]))

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
       (fact "the answers are retrieved in the requested order"
             (let [{o-id :objective-id q-id :_id :as question} (sh/store-a-question)
                   question-uri (str "/objectives/" o-id "/questions/" q-id)

                   {first-answer-id :_id} (sh/with-votes (sh/store-an-answer {:question question}) {:up 2 :down 1})
                   {second-answer-id :_id} (sh/with-votes (sh/store-an-answer {:question question}) {})
                   {third-answer-id :_id} (sh/with-votes (sh/store-an-answer {:question question}) {:up 1 :down 2})]
               (answers/get-answers question-uri {:sorted-by :created-at}) => (contains [(contains {:_id third-answer-id})
                                                                                         (contains {:_id second-answer-id})
                                                                                         (contains {:_id first-answer-id})
                                                                                         ])
               
               (answers/get-answers question-uri {:sorted-by :up-votes}) => (contains [(contains {:_id first-answer-id})
                                                                                       (contains {:_id third-answer-id})
                                                                                       (contains {:_id second-answer-id})])
               
               (answers/get-answers question-uri {:sorted-by :down-votes}) => (contains [(contains {:_id third-answer-id})
                                                                                         (contains {:_id first-answer-id})
                                                                                         (contains {:_id second-answer-id})])))

       (fact "filters answers according to the filter-type"
             (let [{o-id :objective-id q-id :_id :as question} (sh/store-a-question)
                   question-url (str "/objectives/" o-id "/questions/" q-id)

                   {answer-without-note-id :_id} (sh/store-an-answer {:question question :answer-text "without note"})
                   {answer-with-note-id :_id} (-> (sh/store-an-answer {:question question :answer-text "with note"})
                                                  sh/with-note)]
               (answers/get-answers question-url {:filter-type :has-writer-note}) => (just [(contains {:_id answer-with-note-id})])
               (answers/get-answers question-url {}) => (contains [(contains {:_id answer-with-note-id})
                                                                   (contains {:_id answer-without-note-id})]
                                                                  :in-any-order)))

       (fact "offsets the number of answers retrieved"
             (let [{o-id :objective-id q-id :_id :as question} (sh/store-a-question)
                   question-url (str "/objectives/" o-id "/questions/" q-id)
                   stored-answers (doall (->> (repeat {:question question})
                                              (take 10)
                                              (map sh/store-an-answer)))]
             (count (answers/get-answers question-url {:offset 7})) => 3))

       (fact "gets answers with aggregate votes"
             (let [{o-id :objective-id q-id :_id :as question} (sh/store-a-question)
                   question-uri (str "/objectives/" o-id "/questions/" q-id)

                   answer (sh/with-votes (sh/store-an-answer {:question question}) {:up 10 :down 5})]
               (first (answers/get-answers question-uri {:sorted-by :created-at})) => (contains {:votes {:up 10 :down 5}})))

       (fact "gets answers with uris rather than global ids"
             (let [{o-id :objective-id q-id :_id :as question} (sh/store-a-question)
                   question-uri (str "/objectives/" o-id "/questions/" q-id)

                   answer (sh/store-an-answer {:question question})
                   answer-uri (str question-uri "/answers/" (:_id answer))]
               (first (answers/get-answers question-uri {:sorted-by :created-at})) => (contains {:uri answer-uri})
               (first (answers/get-answers question-uri {:sorted-by :created-at})) =not=> (contains {:global-id anything}))))
