(ns objective8.integration.back-end.writer-notes
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.back-end.actions :as actions]
            [objective8.core :as core]
            [objective8.back-end.domain.writers :as writers]
            [objective8.back-end.domain.writer-notes :as writer-notes]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.middleware :as m]))

(def app (helpers/api-context))

(def OBJECTIVE_ID 234)
(def USER_ID 1)

(def section-label "1234abcd")
(def section [["h1" {:data-section-label section-label} "A Heading"]])
(def draft-content [["h1" {:data-section-label section-label} "A Heading"] ["p" {:data-section-label "abcd1234"} "A paragraph"]])

(facts "POST /api/v1/meta/writer-notes"
       (against-background
         (m/valid-credentials? anything anything anything) => true)

       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "the posted note is stored against an answer"
               (against-background
                   (actions/writer-for-objective? anything anything) => true)
               (let [{user-id :_id :as user} (sh/store-a-user)
                     {o-id :objective-id a-id :_id q-id :question-id global-id :global-id} (sh/store-an-answer)
                     uri-for-answer (str "/objectives/" o-id "/questions/" q-id "/answers/" a-id)
                     note-data {:note-on-uri uri-for-answer
                                :note "A note"
                                :created-by-id user-id}
                     {response :response} (p/request app (utils/api-path-for :api/post-writer-note)
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body (json/generate-string note-data))]
                 (:status response) => 201
                 (:body response) => (helpers/json-contains {:_id integer?
                                                             :uri (contains "/notes/")
                                                             :note-on-uri uri-for-answer
                                                             :note "A note"
                                                             :objective-id o-id
                                                             :created-by-id user-id})
                 (:body response) =not=> (helpers/json-contains {:note-on-id anything})
                 (:body response) =not=> (helpers/json-contains {:global-id anything})
                 (:headers response) => (helpers/location-contains (str "/api/v1/meta/writer-notes/"))))

         (fact "the posted note is stored against an annotation"
               (against-background
                 (actions/writer-for-objective? anything anything) => true)
               (let [{user-id :_id :as user} (sh/store-a-user)
                     {draft-id :_id objective-id :objective-id :as draft} (sh/store-a-draft {:content draft-content})
                     section-data {:objective-id objective-id
                                   :draft-id draft-id
                                   :section-label section-label}
                     comment-for-this-section {:comment "section comment" :reason "general"
                                               :created-by-id (:submitter-id draft)}
                     stored-comment (actions/create-section-comment! section-data comment-for-this-section)
                     comment-uri (-> stored-comment :result :uri)
                     note-data {:note-on-uri comment-uri
                                :note "A note"
                                :created-by-id user-id}
                     {response :response} (p/request app (utils/api-path-for :api/post-writer-note)
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body (json/generate-string note-data))]
                 (:status response) => 201
                 (:body response) => (helpers/json-contains {:_id integer?
                                                             :uri (contains "/notes/")
                                                             :note-on-uri comment-uri
                                                             :note "A note"
                                                             :objective-id objective-id
                                                             :created-by-id user-id})
                 (:body response) =not=> (helpers/json-contains {:note-on-id anything})
                 (:body response) =not=> (helpers/json-contains {:global-id anything})
                 (:headers response) => (helpers/location-contains (str "/api/v1/meta/writer-notes/"))))

         (fact "the posted note is stored against a comment"
               (against-background
                   (actions/writer-for-objective? anything anything) => true)
               (let [{user-id :_id :as user} (sh/store-a-user)
                     {o-id :objective-id c-id :_id :as comment} (sh/store-a-comment)
                     uri-for-comment (str "/comments/" c-id)
                     note-data {:note-on-uri uri-for-comment
                                :note "A note"
                                :created-by-id user-id}
                     {response :response} (p/request app (utils/api-path-for :api/post-writer-note)
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body (json/generate-string note-data))]
                 (:status response) => 201
                 (:body response) => (helpers/json-contains {:_id integer?
                                                             :uri (contains "/notes/")
                                                             :note-on-uri uri-for-comment
                                                             :note "A note"
                                                             :objective-id o-id
                                                             :created-by-id user-id})
                 (:body response) =not=> (helpers/json-contains {:note-on-id anything})
                 (:body response) =not=> (helpers/json-contains {:global-id anything})
                 (:headers response) => (helpers/location-contains (str "/api/v1/meta/writer-notes/"))))

         (fact "returns 404 when entity to be noted on doesn't exist"
               (let [note-data {:note-on-uri "nonexistent/entity"
                                :note "A note"
                                :created-by-id 1}
                     {response :response} (p/request app (utils/api-path-for :api/post-writer-note)
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body (json/generate-string note-data))]
                 (:status response) => 404
                 (:body response) => (helpers/json-contains {:reason "Entity does not exist"})))))

