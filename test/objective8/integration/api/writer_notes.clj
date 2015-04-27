(ns objective8.integration.api.writer-notes 
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.actions :as actions]
            [objective8.core :as core]
            [objective8.writers :as writers]
            [objective8.writer-notes :as writer-notes]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.middleware :as m]))

(def app (helpers/test-context))

(def OBJECTIVE_ID 234)
(def USER_ID 1)


(facts "POST /api/v1/meta/writer-notes"
       (against-background
         (m/valid-credentials? anything anything anything) => true)
        
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]
         (fact "the posted note is stored"
               (against-background
                   (actions/writer-for-objective? anything anything) => true)
               (let [{user-id :_id :as user} (sh/store-a-user)
                     {o-id :objective-id a-id :_id q-id :question-id global-id :global-id} (sh/store-an-answer)
                     uri-for-answer (str "/objectives/" o-id "/questions/" q-id "/answers/" a-id)
                     note-data {:note-on-uri uri-for-answer
                                :note "A note"
                                :created-by-id user-id}
                     {response :response} (p/request app (str "/api/v1/meta/writer-notes")
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

         (fact "returns 404 when entity to be noted on doesn't exist"
               (let [note-data {:note-on-uri "nonexistent/entity"
                                :note "A note"
                                :created-by-id 1}
                     {response :response} (p/request app (str "/api/v1/meta/writer-notes")
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body (json/generate-string note-data))]
                 (:status response) => 404
                 (:body response) => (helpers/json-contains {:reason "Entity does not exist"})))))

