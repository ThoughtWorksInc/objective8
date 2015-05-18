(ns objective8.integration.back-end.up-down-votes
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.core :as core]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as ih]
            [objective8.integration.storage-helpers :as sh]
            [objective8.middleware :as m]))

(def app (ih/test-context))
(binding [config/two-phase? true]  
  (facts "POST /api/v1/up-down-votes"
         (against-background
           (m/valid-credentials? anything anything anything) => true) 
         (against-background
           [(before :contents (do (ih/db-connection)
                                  (ih/truncate-tables)))
            (after :facts (ih/truncate-tables))]

           (fact "A user can upvote an answer" ;; Make this generic
                 (let [{voting-user-id :_id} (sh/store-a-user)
                       {o-id :objective-id q-id :question-id a-id :_id} (sh/store-an-answer)
                       uri (str "/objectives/" o-id "/questions/" q-id "/answers/" a-id)
                       {response :response} (p/request app (utils/path-for :be/post-up-down-vote)
                                                       :request-method :post
                                                       :content-type "application/json"
                                                       :body (json/generate-string {:created-by-id voting-user-id
                                                                                    :vote-on-uri uri
                                                                                    :vote-type :up}))]
                   (:status response) => 200))


           (fact "A second vote by the same user on the same entity is not permitted"
                 (let [{voting-user-id :_id} (sh/store-a-user)
                       {o-id :objective-id q-id :question-id a-id :_id} (sh/store-an-answer)
                       uri (str "/objectives/" o-id "/questions/" q-id "/answers/" a-id)
                       {response :response} (-> app
                                                (p/request (utils/path-for :be/post-up-down-vote)
                                                           :request-method :post
                                                           :content-type "application/json"
                                                           :body (json/generate-string {:created-by-id voting-user-id
                                                                                        :vote-on-uri uri
                                                                                        :vote-type :up}))
                                                (p/request (utils/path-for :be/post-up-down-vote)
                                                           :request-method :post
                                                           :content-type "application/json"
                                                           :body (json/generate-string {:created-by-id voting-user-id
                                                                                        :vote-on-uri uri
                                                                                        :vote-type :down})))]
                   (:status response) => 403))

           (fact "A vote can be cast against a comment on a draft"
                 (let [{voting-user-id :_id} (sh/store-a-user)
                       draft (sh/store-a-draft)
                       the-comment (sh/store-a-comment {:entity draft})

                       uri (str "/comments/" (:_id the-comment))
                       {response :response} (p/request app (utils/path-for :be/post-up-down-vote)
                                                       :request-method :post
                                                       :content-type "application/json"
                                                       :body (json/generate-string {:created-by-id voting-user-id
                                                                                    :vote-on-uri uri
                                                                                    :vote-type :up}))]
                   (:status response) => 200))

           (fact "A vote can be cast against a comment on an open objective"
                 (let [{voting-user-id :_id} (sh/store-a-user)
                       objective (sh/store-an-open-objective)
                       the-comment (sh/store-a-comment {:entity objective})

                       uri (str "/comments/" (:_id the-comment))
                       {response :response} (p/request app (utils/path-for :be/post-up-down-vote)
                                                       :request-method :post
                                                       :content-type "application/json"
                                                       :body (json/generate-string {:created-by-id voting-user-id
                                                                                    :vote-on-uri uri
                                                                                    :vote-type :up}))]
                   (:status response) => 200))

           (fact "A vote cannot be cast against a comment on an objective that is in drafting"
                 (let [{voting-user-id :_id} (sh/store-a-user)
                       {o-id :_id :as objective} (sh/store-an-open-objective)
                       the-comment (sh/store-a-comment {:entity objective})

                       _ (sh/start-drafting! o-id)

                       uri (str "/comments/" (:_id the-comment))
                       {response :response} (p/request app (utils/path-for :be/post-up-down-vote)
                                                       :request-method :post
                                                       :content-type "application/json"
                                                       :body (json/generate-string {:created-by-id voting-user-id
                                                                                    :vote-on-uri uri
                                                                                    :vote-type :up}))]
                   (:status response) => 403))))) 
