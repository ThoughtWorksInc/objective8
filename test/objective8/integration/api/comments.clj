(ns objective8.integration.api.comments
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.middleware :as m]
            [objective8.comments :as comments]))

;; Testing from http request -> making correct calls within comments namespace
;; Mock or stub out 'comments' namespace

(def app (helpers/test-context))

(def OBJECTIVE_ID 234)
(def USER_ID 1)

(def the-comment {:comment "The comment"
                  :objective-id OBJECTIVE_ID
                  :created-by-id USER_ID})

(def the-comment-as-json (json/generate-string the-comment))
(def the-invalid-comment {:comment "The comment"
                          :objective-id OBJECTIVE_ID })

(defn a-comment [comment-on-id objective-id created-by-id]
  {:comment "The comment"
   :objective-id objective-id
   :comment-on-id comment-on-id
   :created-by-id created-by-id})

(facts "POST /api/v1/meta/comments"
       (against-background
        (m/valid-credentials? anything anything anything) => true)
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]
        (fact "the posted comment is stored"
              (let [{user-id :_id :as user} (sh/store-a-user)
                    {o-id :objective-id d-id :_id global-id :global-id} (sh/store-a-draft)
                    uri-for-draft (str "/objectives/" o-id "/drafts/" d-id)
                    comment-data {:comment-on-uri uri-for-draft
                                  :comment "A comment"
                                  :created-by-id user-id}
                    {response :response} (p/request app (str "/api/v1/meta/comments")
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string comment-data))]
                (:status response) => 201
                (:body response) => (helpers/json-contains {:_id integer?
                                                            :comment-on-uri uri-for-draft
                                                            :comment "A comment"
                                                            :created-by-id user-id})
                (:body response) =not=> (helpers/json-contains {:comment-on-id anything})
                (:headers response) => (helpers/location-contains (str "/api/v1/meta/comments/"))))

        (fact "returns 404 when entity to be commented on doesn't exist"
              (let [comment-data {:comment-on-uri "nonexistent/entity"
                                  :comment "A comment"
                                  :created-by-id 1}
                    {response :response} (p/request app (str "/api/v1/meta/comments")
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string comment-data))]
                (:status response) => 404
                (:body response) => (helpers/json-contains {:reason "Entity does not exist"})))))

(facts "GET /api/v1/meta/comments?uri=<uri>"
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]
        (fact "retrieves comments for the entity at <uri>"
              (let [user (sh/store-a-user)
                    {draft-id :_id objective-id :objective-id :as draft} (sh/store-a-draft)
                    draft-uri (str "/objectives/" objective-id "/drafts/" draft-id)
                    stored-comments (doall (->> (repeat {:entity draft :user user})
                                                (take 5)
                                                (map sh/store-a-comment)
                                                (map #(dissoc % :username :comment-on-id))
                                                (map #(assoc % :comment-on-uri draft-uri))))
                    escaped-draft-uri (str "%2fobjectives%2f" objective-id "%2fdrafts%2f" draft-id)
                    {response :response} (p/request app (str "/api/v1/meta/comments?uri=" escaped-draft-uri))]
                (:body response) => (helpers/json-contains (map contains stored-comments))))))
