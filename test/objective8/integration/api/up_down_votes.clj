(ns objective8.integration.api.up-down-votes
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.core :as core]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]
            [objective8.middleware :as m]))

(def app (ih/test-context))

(facts "POST /api/v1/up-down-votes"
       (against-background
        (m/valid-credentials? anything anything anything) => true)
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

        (fact "Upvotes an answer" ;; Make this generic
                     (let [{user-id :_id} (sh/store-a-user)
                           {global-id :global-id} (sh/store-an-answer)
                           {response :response} (p/request app (utils/path-for :api/post-up-down-vote)
                                                           :request-method :post
                                                           :content-type "application/json"
                                                           :body (json/generate-string {:created-by-id user-id
                                                                                        :global-id global-id
                                                                                        :vote-type :up}))]
                       (:status response) => 200))
        
        
        (fact "A second vote on the same entity is not permitted"
              (let [{global-id :global-id created-by-id :created-by-id} (sh/store-an-answer)
                           {response :response} (-> app
                                                    (p/request (utils/path-for :api/post-up-down-vote)
                                                               :request-method :post
                                                               :content-type "application/json"
                                                               :body (json/generate-string {:created-by-id created-by-id
                                                                                            :global-id global-id
                                                                                            :vote-type :up}))
                                                    (p/request (utils/path-for :api/post-up-down-vote)
                                                               :request-method :post
                                                               :content-type "application/json"
                                                               :body (json/generate-string {:created-by-id created-by-id
                                                                                            :global-id global-id
                                                                                            :vote-type :down})))]
                       (:status response) => 403
                       ;; Should also provide a reason for failure in the response
                       ))))
