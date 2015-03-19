(ns objective8.integration.api.drafting
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.drafts :as drafts]
            [objective8.middleware :as m]))

(def app (helpers/test-context))

(facts "POST /dev/api/v1/objectives/obj-id/start-drafting"
  (against-background
      (m/valid-credentials? anything anything anything) => true)
  (against-background
    [(before :contents (do (helpers/db-connection)
                           (helpers/truncate-tables)))
     (after :facts (helpers/truncate-tables))]
    
    (fact "drafting-started flag for objective set to true" 
      (let [{objective-id :_id} (sh/store-an-objective)
            {response :response} (p/request app (str "/dev/api/v1/objectives/" objective-id "/start-drafting")
                                   :request-method :post
                                   :content-type "application/json")]
        (:body response) => (helpers/json-contains {:drafting-started true})))

    (fact "active invitations status set to expired"
      (let [{objective-id :_id :as objective} (sh/store-an-objective)
            {active-invitation-id :_id} (sh/store-an-invitation {:objective objective})
            {accepted-invitation-id :_id} (sh/store-an-invitation {:objective objective :status "accepted"})

            {active-invitation-for-other-objective-id :_id} (sh/store-an-invitation)

            {response :response} (p/request app (str "/dev/api/v1/objectives/" objective-id "/start-drafting")
                                   :request-method :post
                                   :content-type "application/json")]
        (:status (sh/retrieve-invitation active-invitation-id)) => "expired"
        (:status (sh/retrieve-invitation accepted-invitation-id)) => "accepted"

        (:status (sh/retrieve-invitation active-invitation-for-other-objective-id)) => "active"))))

(facts "POST /dev/api/v1/objectives/:id/drafts"
  (against-background
    (m/valid-credentials? anything anything anything) => true)
  (against-background
    [(before :contents (do (helpers/db-connection)
                           (helpers/truncate-tables)))
     (after :facts (helpers/truncate-tables))]
    
    (fact "creates a draft when submitter id is a writer for the objective and drafting has started"
          (let [{objective-id :objective-id submitter-id :user-id} (sh/store-a-candidate)
                _ (sh/start-drafting! objective-id)
                the-draft {:objective-id objective-id
                           :submitter-id submitter-id
                           :content "Some content"}
                {response :response} (p/request app (utils/path-for :api/post-draft :id objective-id)
                                            :request-method :post
                                            :content-type "application/json"
                                            :body (json/generate-string the-draft))]
            (:body response) => (helpers/json-contains {:_id anything
                                                        :objective-id objective-id
                                                        :submitter-id submitter-id
                                                        :content "Some content"})
            (:status response) => 201))

    (fact "a draft is not created when drafting has not started"
          (let [{objective-id :objective-id submitter-id :user-id} (sh/store-a-candidate)
                the-draft {:objective-id objective-id
                           :submitter-id submitter-id
                           :content "Some content"}
                {response :response} (p/request app (utils/path-for :api/post-draft :id objective-id)
                                                :request-method :post
                                                :content-type "application/json"
                                                :body (json/generate-string the-draft))]
            (:status response) => 404))

    (fact "a draft is not created when submitter id is not a writer for the objective"
          (let [{objective-id :_id :as objective} (sh/store-an-objective)
                {submitter-id :_id} (sh/store-a-user)
                the-draft {:objective-id objective-id
                           :submitter-id submitter-id 
                           :content "Some content"}
                {response :response} (p/request app (utils/path-for :api/post-draft :id objective-id)
                                                :request-method :post
                                                :content-type "application/json"
                                                :body (json/generate-string the-draft))]
            (:status response) => 404)))) 

(facts "GET /dev/api/v1/objectives/:id/drafts/:d-id"
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "gets a draft for an objective"
              (let [{objective-id :objective-id draft-id :_id :as draft} (sh/store-a-draft)
                    {response :response} (p/request app (utils/path-for :api/get-draft :id objective-id 
                                                                        :d-id draft-id))]
                (:status response) => 200
                (:body response) => (helpers/json-contains (dissoc draft :username))))))

(facts "GET /dev/api/v1/objectives/:id/drafts"
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "gets drafts for an objective"
              (let [objective (sh/store-an-objective)
                    stored-drafts (doall (->> (repeat {:objective objective})
                                              (take 5)
                                              (map sh/store-a-draft)
                                              (map #(dissoc % :username))))
                    {response :response} (p/request app (utils/path-for :api/get-drafts-for-objective :id (:_id objective)))]
                (:status response) => 200
                (:body response) => (helpers/json-contains (map contains (reverse stored-drafts)))))))

(facts "GET /dev/api/v1/objectives/:id/drafts/current"
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

        (fact "gets a draft for an objective"
              (let [{objective-id :objective-id :as draft} (sh/store-a-draft)
                    {response :response} (p/request app (utils/path-for :api/get-draft :id objective-id 
                                                                        :d-id "current"))]
                (:status response) => 200
                (:body response) => (helpers/json-contains (dissoc draft :username))))))
