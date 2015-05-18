(ns objective8.integration.db.writer-notes 
  (:require [midje.sweet :refer :all]
            [objective8.back-end.storage.domain.writer-notes :as writer-notes]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]))

(facts "about storing writer-notes"
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

        (fact "notes can be stored against an answer"
              (let [{user-id :_id :as user} (sh/store-a-user)
                    {o-id :objective-id a-id :_id q-id :question-id :as answer} (sh/store-an-answer)
                    uri-for-answer (str "/objectives/" o-id "/questions/" q-id "/answers/" a-id)
                    note-data {:note-on-uri uri-for-answer 
                               :note "A note"
                               :created-by-id user-id}]

                (writer-notes/store-note-for! answer note-data) => (contains {:_id integer?
                                                                              :uri (contains "/notes/")
                                                                              :note-on-uri uri-for-answer 
                                                                              :note "A note"
                                                                              :created-by-id user-id})
                (writer-notes/store-note-for! answer note-data) =not=> (contains {:note-on-id anything})
                (writer-notes/store-note-for! answer note-data) =not=> (contains {:global-id anything})))))

(facts "about getting writer-note by entity uri"
       (against-background
         [(before :contents (do (ih/db-connection)
                                (ih/truncate-tables)))
          (after :facts (ih/truncate-tables))]

         (fact "get writer-note by entity uri"
               (let [{o-id :objective-id a-id :_id q-id :question-id :as answer} (sh/store-an-answer)
                     uri-for-answer (str "/objectives/" o-id "/questions/" q-id "/answers/" a-id)
                     {:keys [note _id objective-id _created_at created-by-id entity]} (sh/store-a-note {:note-on-entity answer})]

                 (writer-notes/retrieve-note uri-for-answer) => (contains {:_id _id
                                                                           :objective-id objective-id
                                                                           :entity entity
                                                                           :created-by-id created-by-id
                                                                           :_created_at _created_at
                                                                           :note note})
                 (writer-notes/retrieve-note uri-for-answer) =not=> (contains {:note-on-id anything})
                 (writer-notes/retrieve-note uri-for-answer) =not=> (contains {:global-id anything})))))
