(ns objective8.integration.back-end.drafting
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.config :as config]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.back-end.domain.drafts :as drafts]
            [objective8.back-end.actions :as actions]
            [objective8.middleware :as m]))

(def app (helpers/test-context))

(def some-hiccup [["p" "tiny paragraph"]])


(facts "POST /api/v1/objectives/:id/drafts"
       (against-background
         (m/valid-credentials? anything anything anything) => true) 
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "creates a draft when submitter id is a writer for the objective and drafting has started"
               (let [{objective-id :objective-id submitter-id :user-id} (sh/store-a-writer)
                     draft-data {:objective-id objective-id
                                 :submitter-id submitter-id
                                 :content some-hiccup}
                     {response :response :as peridot-response} (p/request app (utils/path-for :api/post-draft :id objective-id)
                                                                          :request-method :post
                                                                          :content-type "application/json"
                                                                          :body (json/generate-string draft-data))]
                 (:body response) => (helpers/json-contains {:_id anything
                                                             :objective-id objective-id
                                                             :submitter-id submitter-id
                                                             :content (just [(just ["p" (just {:data-section-label (just #"[0-9a-f]{8}")}) "tiny paragraph"])])}) 
                 (:status response) => 201))

         (fact "a draft is not created when submitter id is not a writer for the objective"
               (let [{objective-id :_id :as objective} (sh/store-an-open-objective)
                     {submitter-id :_id} (sh/store-a-user)
                     the-draft {:objective-id objective-id
                                :submitter-id submitter-id 
                                :content some-hiccup}
                     {response :response} (p/request app (utils/path-for :api/post-draft :id objective-id)
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body (json/generate-string the-draft))]
                 (:status response) => 404))))

(facts "GET /api/v1/objectives/:id/drafts/:d-id"
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

         (fact "gets a draft for an objective with comment count included"
               (let [{objective-id :objective-id draft-id :_id :as stored-draft} (sh/store-a-draft)
                     draft (drafts/retrieve-draft draft-id)
                     _ (sh/store-a-comment {:entity stored-draft})
                     {response :response} (p/request app (utils/path-for :api/get-draft :id objective-id 
                                                                         :d-id draft-id))]
                 (:status response) => 200
                 (:body response) => (helpers/json-contains 
                                       (assoc draft :meta {:comments-count 1
                                                           :annotations-count 0}))))))


(facts "GET /api/v1/objectives/:id/drafts"
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "gets drafts for an objective"
               (let [objective (sh/store-an-objective-in-draft)
                     stored-drafts (doall (->> (repeat {:objective objective})
                                               (take 5)
                                               (map sh/store-a-draft)
                                               (map #(dissoc % :_created_at_sql_time))))
                     {response :response} (p/request app (utils/path-for :api/get-drafts-for-objective :id (:_id objective)))]
                 (:status response) => 200
                 (:body response) => (helpers/json-contains (map contains (-> stored-drafts :_id reverse)))))))

(facts "GET /api/v1/objectives/:id/drafts/latest"
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "gets the latest draft for an objective with comment count included"
               (let [{objective-id :_id :as objective} (sh/store-an-objective-in-draft)

                     first-draft (sh/store-a-draft {:objective objective})
                     {second-draft-id :draft-id :as stored-draft} (sh/store-a-draft {:objective objective})

                     latest-draft (drafts/retrieve-draft second-draft-id)
                     _ (sh/store-a-comment {:entity stored-draft})
                     
                     {response :response} (p/request app (utils/path-for :api/get-draft :id objective-id
                                                                         :d-id "latest"))]
                 (:status response) => 200
                 (:body response) => (helpers/json-contains 
                                       (assoc latest-draft :meta {:comments-count 1
                                                                  :annotations-count 0}))))
         
         (fact "returns draft-id for previous draft"
               (let [objective (sh/store-an-objective-in-draft)
                     {first-draft-id :_id} (sh/store-a-draft {:objective objective})
                     {second-draft-id :_id :as second-draft} (sh/store-a-draft {:objective objective})]
                 
                 (get-in (p/request app (utils/path-for :api/get-draft
                                                        :id (:_id objective)
                                                        :d-id "latest"))
                         [:response :body]) => (helpers/json-contains {:previous-draft-id first-draft-id})))))

(def section-label "1234abcd")
(def section [["h1" {:data-section-label section-label} "A Heading"]])
(def draft-content [["h1" {:data-section-label section-label} "A Heading"] ["p" {:data-section-label "abcd1234"} "A paragraph"]])

(facts "GET /api/v1/objectives/:id/drafts/:d-id/sections/:s-label"
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]
         
         (fact "gets section of draft"
               (let [{draft-id :_id objective-id :objective-id :as draft} (sh/store-a-draft {:content draft-content})] 
                 (get-in (p/request app (utils/path-for :api/get-section
                                                        :id objective-id
                                                        :d-id draft-id
                                                        :section-label section-label))
                         [:response :body]) => (helpers/json-contains {:section section
                                                                       :uri (str "/objectives/" objective-id
                                                                                 "/drafts/" draft-id
                                                                                 "/sections/" section-label)
                                                                       :objective-id objective-id})))))

(facts "GET /api/v1/objectives/:id/drafts/:d-id/sections"
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "gets sections of draft with annotation counts"
               (let [{draft-id :_id objective-id :objective-id :as draft} (sh/store-a-draft {:content draft-content}) 
                     section-data {:objective-id objective-id
                                   :draft-id draft-id
                                   :section-label section-label} 
                     comment-for-this-section {:comment "section comment" :reason "general" :created-by-id (:submitter-id draft)}] 
                 (actions/create-section-comment! section-data comment-for-this-section)
                 
                 (get-in (p/request app (utils/path-for :api/get-sections
                                                        :id objective-id
                                                        :d-id draft-id))
                         [:response :body]) => (helpers/json-contains [(contains {:section-label section-label
                                                                                  :meta {:annotations-count 1}})])))))

(facts "GET /api/v1/objectives/:id/drafts/:d-id/annotations" 
       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]
         
         (fact "gets all draft annotations with the annotated section" 
               (let [{draft-id :_id objective-id :objective-id :as draft} (sh/store-a-draft {:content draft-content}) 
                     section-data {:objective-id objective-id
                                   :draft-id draft-id
                                   :section-label section-label} 
                     comment-for-this-section {:comment "section comment" :reason "general" :created-by-id (:submitter-id draft)}
                     _ (actions/create-section-comment! section-data comment-for-this-section)] 
                 (get-in (p/request app (utils/path-for :api/get-annotations
                                                        :id objective-id
                                                        :d-id draft-id))
                         [:response :body]) => (helpers/json-contains 
                                                 [(just 
                                                    {:section section
                                                     :uri (str "/objectives/" objective-id
                                                               "/drafts/" draft-id
                                                               "/sections/" section-label)
                                                     :objective-id objective-id
                                                     :comments (just 
                                                                 [(contains (dissoc comment-for-this-section :reason))])})])))))
