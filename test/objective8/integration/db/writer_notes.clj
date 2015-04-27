(ns objective8.integration.db.writer-notes 
  (:require [midje.sweet :refer :all]
            [objective8.writer-notes :as writer-notes]
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
