(ns objective8.integration.api.up-down-votes
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.core :as core]
            [objective8.utils :as utils]
            [objective8.integration-helpers :as ih]
            [objective8.storage-helpers :as sh]
            [objective8.middleware :as m]))

(def app (ih/test-context))

(facts "POST /api/v1/up-down-votes"
       (against-background
        (m/valid-credentials? anything anything anything) => true)
       (against-background
        [(before :contents (do (ih/db-connection)
                               (ih/truncate-tables)))
         (after :facts (ih/truncate-tables))]

       (future-fact "Upvotes an answer" ;; Make this generic
             (let [{ueid :ueid user-id :user-id} (sh/store-an-answer)
                   {response :response} (p/request app (utils/path-for :api/post-up-down-vote)
                                                   :request-method :post
                                                   :content-type "application/json"
                                                   :body (json/generate-string {:user-id user-id
                                                                                :ueid ueid
                                                                                :vote-type :up}))]
               (:status response) => 200))
       

       (future-fact "A second upvote on the same answer is not permitted"
              (let [{ueid :ueid user-id :user-id} (sh/store-an-answer)
                    {response :response} (-> app
                                             (p/request (utils/path-for :api/post-up-down-vote)
                                                        :request-method :post
                                                        :content-type "application/json"
                                                        :body (json/generate-string {:user-id user-id
                                                                                     :ueid ueid
                                                                                     :vote-type :up}))
                                             (p/request (utils/path-for :api/post-up-down-vote)
                                                        :request-method :post
                                                        :content-type "application/json"
                                                        :body (json/generate-string {:user-id user-id
                                                                                     :ueid ueid
                                                                                     :vote-type :up})))]
                (:status response) => 403
                ;; Should also provide a reason for failure in the response
                ))))
