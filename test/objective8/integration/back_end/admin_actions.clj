(ns objective8.integration.back-end.admin-actions
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.middleware :as m]))

(def app (helpers/api-context))

(facts "POST /api/v1/meta/admin-removals"
       (against-background
        (m/valid-credentials? anything anything anything) => true)
       (against-background
        [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]
        (fact "the posted admin removal is stored"
              (let [admin-uri (->> (sh/store-an-admin)
                                  :_id
                                  (str "/users/"))
                    objective-uri (->> (sh/store-an-objective)
                                       :_id
                                       (str "/objectives/"))
                    removal-data {:removal-uri objective-uri
                                  :removed-by-uri admin-uri}
                    {response :response} (p/request app (utils/api-path-for :api/post-admin-removal)
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string removal-data))]
                (:status response) => 201
                (:body response) => (helpers/json-contains {:uri (contains "/admin-removals/")
                                                            :removal-uri objective-uri
                                                            :removed-by-uri admin-uri})
                (:headers response) => (helpers/location-contains "/api/v1/meta/admin-removals/")))

        (fact "returns 404 when entity to be removed doesn't exist"
              (let [admin-uri (->> (sh/store-an-admin)
                                  :_id
                                  (str "/users/"))
                    removal-data {:removal-uri "/non-existent-objective"
                                  :removed-by-uri admin-uri}
                    {response :response} (p/request app (utils/api-path-for :api/post-admin-removal)
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string removal-data))]
                (:status response) => 404
                (:body response) => (helpers/json-contains {:reason "Entity with that uri does not exist for removal"})))

         (fact "returns 404 when entity has already been removed"
              (let [admin-uri (->> (sh/store-an-admin)
                                  :_id
                                  (str "/users/"))
                    objective-uri (->> (sh/store-an-admin-removed-objective)
                                       :_id
                                       (str "/objectives/"))
                    removal-data {:removal-uri objective-uri
                                  :removed-by-uri admin-uri}
                    {response :response} (p/request app (utils/api-path-for :api/post-admin-removal)
                                                    :request-method :post
                                                    :content-type "application/json"
                                                    :body (json/generate-string removal-data))]
                (:status response) => 404
                (:body response) => (helpers/json-contains {:reason "Entity with that uri does not exist for removal"})))

         (fact "removal is not posted if the user is not an admin"
               (let [user-uri (->> (sh/store-a-user)
                                   :_id
                                   (str "/users/"))
                     objective-uri (->> (sh/store-an-objective)
                                        :_id
                                        (str "/objectives/"))
                     removal-data {:removal-uri objective-uri
                                   :removed-by-uri user-uri}
                     {response :response} (p/request app (utils/api-path-for :api/post-admin-removal)
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body (json/generate-string removal-data))]
                 (:status response) => 403
                 (:body response) => (helpers/json-contains {:reason "Admin credentials are required for this request"})))))

(facts "GET /api/v1/meta/admin-removals"
       (against-background
         [(before :contents (do (helpers/db-connection)
                               (helpers/truncate-tables)))
         (after :facts (helpers/truncate-tables))]

         (fact "retrieves admin-removals"
              (let [stored-admin-removal (sh/store-an-admin-removal)
                    expected-retrieved-admin-removal (-> stored-admin-removal
                                                         (assoc :uri (str "/admin-removals/" (:_id stored-admin-removal))
                                                                :removed-by-uri (str "/users/" (:removed-by-id stored-admin-removal)))
                                                         (dissoc :_id :removed-by-id))
                    {response :response} (p/request app (utils/api-path-for :api/get-admin-removals))]
                    (:body response)  => (helpers/json-contains [expected-retrieved-admin-removal])))))

(facts "about /api/v1/meta/promote-objective"
       (against-background
         (m/valid-credentials? anything anything anything) => true)

       (against-background
         [(before :contents (do (helpers/db-connection)
                                (helpers/truncate-tables)))
          (after :facts (helpers/truncate-tables))]

         (fact "the promoted objective is stored"
               (let [admin-uri (->> (sh/store-an-admin)
                                    :_id
                                    (str "/users/"))
                     objective-uri (->> (sh/store-an-objective)
                                        :_id
                                        (str "/objectives/"))
                     objective-data {:objective-uri objective-uri
                                     :promoted-by admin-uri}
                     {response :response} (p/request app (utils/api-path-for :api/put-promote-objective)
                                                     :request-method :put
                                                     :content-type "application/json"
                                                     :body (json/generate-string objective-data))]

                 (:status response) => 200
                 (:body response) => (helpers/json-contains {:uri objective-uri
                                                             :promoted true})
                 (:headers response) => (helpers/location-contains "/api/v1/meta/promote-objective/")))

         (fact "returns 403 if the user is not an admin"
               (let [user-uri (->> (sh/store-a-user)
                                    :_id
                                    (str "/users/"))
                     objective-uri (->> (sh/store-an-objective)
                                        :_id
                                        (str "/objectives/"))
                     objective-data {:objective-uri objective-uri
                                     :promoted-by user-uri}
                     {response :response} (p/request app (utils/api-path-for :api/put-promote-objective)
                                                     :request-method :put
                                                     :content-type "application/json"
                                                     :body (json/generate-string objective-data))]

                 (:status response) => 403
                 (:body response) => (helpers/json-contains {:reason "Admin credentials are required for this request. Maximum 3 objectives may be promoted."})))

         (fact "returns 404 if the objective does not exist"
               (let [admin-uri (->> (sh/store-an-admin)
                                    :_id
                                    (str "/users/"))
                     objective-data {:objective-uri "/objectives/1234567890"
                                     :promoted-by admin-uri}
                     {response :response} (p/request app (utils/api-path-for :api/put-promote-objective)
                                                     :request-method :put
                                                     :content-type "application/json"
                                                     :body (json/generate-string objective-data))]

                 (:status response) => 404
                 (:body response) => (helpers/json-contains {:reason "Objective with that URI does not exist"})))

         (fact "returns invalid response if post request is invalid"
               (let [admin-id (->> (sh/store-an-admin)
                                   :_id)
                     admin-uri (str "/users/" admin-id)
                     objective-uri (->> (sh/store-an-objective)
                                        :_id
                                        (str "/objectives/"))
                     objective-data {}
                     {response :response} (p/request app (utils/api-path-for :api/put-promote-objective)
                                                     :request-method :put
                                                     :content-type "application/json"
                                                     :body (json/generate-string objective-data))]

                 (:status response) => 400
               (:body response) => (helpers/json-contains {:reason "Invalid promote objective post request"})))

         (fact "returns 403 if there are at least 3 promoted objectives"
               (let [user-uri (->> (sh/store-an-admin)
                                   :_id
                                   (str "/users/"))
                     objective-uri (->> (sh/store-an-objective)
                                        :_id
                                        (str "/objectives/"))
                     _ (sh/store-a-promoted-objective)
                     _ (sh/store-a-promoted-objective)
                     _ (sh/store-a-promoted-objective)
                     objective-data {:objective-uri objective-uri
                                     :promoted-by user-uri}
                     {response :response} (p/request app (utils/api-path-for :api/put-promote-objective)
                                                     :request-method :put
                                                     :content-type "application/json"
                                                     :body (json/generate-string objective-data))]

                 (:status response) => 403
                 (:body response) => (helpers/json-contains {:reason "Admin credentials are required for this request. Maximum 3 objectives may be promoted."})))))