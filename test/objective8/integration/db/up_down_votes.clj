(ns objective8.integration.db.up-down-votes
  (:require [midje.sweet :refer :all]
            [objective8.back-end.domain.up-down-votes :as votes]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]))

(facts "about storing votes"
       (against-background 
         [(before :contents (do (ih/db-connection)
                                (ih/truncate-tables)))
          (after :facts (ih/truncate-tables))] 

         (fact "votes can be stored against an answer"
               (let [answer-to-vote-on (sh/store-an-answer)
                     {voting-user-id :_id} (sh/store-a-user)
                     uri "answer-uri"
                     vote-data {:vote-on-uri uri :created-by-id voting-user-id :vote-type :up}
                     stored-vote (votes/store-vote! answer-to-vote-on vote-data)]
                 stored-vote => (contains {:_id integer?
                                           :vote-type :up
                                           :created-by-id voting-user-id
                                           :entity :up-down-vote
                                           :vote-on-uri "answer-uri"})
                 stored-vote =not=> (contains {:global-id anything})))))
