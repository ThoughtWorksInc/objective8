(ns objective8.integration.back-end.objectives
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.integration.storage-helpers :as sh]
            [objective8.back-end.domain.objectives :as objectives]
            [objective8.back-end.domain.users :as users]
            [objective8.middleware :as m]))

(def app (helpers/test-context))

(def OBJECTIVE_ID 10)

(def the-objective {:title "my objective title"
                    :description "my objective description"
                    :created-by-id 1})

(def the-invalid-objective {:title "my objective title"
                            :description "my objective description"})

(def stored-objective (assoc the-objective :_id OBJECTIVE_ID))

(defn gen-user-with-id
  "Make a user and return the ID for use in creating other content"
  []
  (:_id (users/store-user! {:twitter-id "anything" :username "username"})))

(background
 (m/valid-credentials? anything anything anything) => true)

(against-background
  [(before :contents (do (helpers/db-connection)
                         #_(helpers/truncate-tables)))
   #_(after :facts (helpers/truncate-tables))]

  (facts "GET /api/v1/objectives returns a list of non-removed objectives in reverse chronological order"
         (fact "objectives are returned as a list"
               (let [stored-objectives (doall (repeatedly 5 sh/store-an-objective))
                     {response :response} (p/request app (utils/api-path-for :api/get-objectives))]
                 (:body response) => (helpers/json-contains (map contains (->> stored-objectives
                                                                               (map #(dissoc % :global-id))
                                                                               reverse)))))

         (fact "returns an empty list if there are no objectives"
               (do
                 (helpers/truncate-tables)
                 (helpers/peridot-response-json-body->map (p/request app (utils/api-path-for :api/get-objectives))))
               => empty?))

  (facts "GET /api/v1/objectives?include-removed=true returns a list of all objectives, including removed"
         (fact "all objectives are returned as a list"
               (let [stored-objectives (doall (repeatedly 2 sh/store-an-objective))
                     stored-removed-objectives (doall (repeatedly 2 sh/store-an-admin-removed-objective))
                     {response :response} (p/request app (str (utils/api-path-for :api/get-objectives)
                                                              "?include-removed=true"))
                     default-response (p/request app (utils/api-path-for :api/get-objectives))]
                 (:body response) => (helpers/json-contains 
                                       (map contains (->> (concat stored-objectives stored-removed-objectives)
                                                          (map #(dissoc % :global-id))
                                                          reverse)))
                 (helpers/peridot-response-json-body->map default-response) 
                 => (just [(contains {:removed-by-admin false}) 
                           (contains {:removed-by-admin false})]))))

  (facts "GET /api/v1/objectives?user-id=<user-id>"
         (fact "returns a list of objectives with meta information for signed-in user"
               (let [unstarred-objective (sh/store-an-objective)
                     {user-id :_id :as user} (sh/store-a-user)
                     starred-objective (sh/store-an-objective)
                     stored-star (sh/store-a-star {:user user :objective starred-objective})
                     {response :response} (p/request app (str (utils/api-path-for :api/get-objectives) "?user-id=" user-id))] 
                 (:body response) => (helpers/json-contains [(contains {:meta (contains {:starred true})})
                                                             (contains {:meta (contains {:starred false})})] :in-any-order))))

  (facts "GET /api/v1/objectives?starred=true&user-id=<user-id>"
         (fact "retrieves objectives in reverse chronological order that have been starred by user with given user-id"
               (let [{user-id :_id :as user} (sh/store-a-user)
                     starred-objectives [(sh/store-an-objective)
                                         (sh/store-an-objective)]
                     unstarred-objective (sh/store-an-objective)

                     stored-stars (doall (map sh/store-a-star
                                              [{:user user :objective (first starred-objectives)}
                                               {:user user :objective (second starred-objectives)}])) ]
                 (get-in (p/request app (str (utils/api-path-for :api/get-objectives) "?starred=true&user-id=" user-id)) [:response :body])
                 => (helpers/json-contains (map contains (->> starred-objectives
                                                              reverse
                                                              (map #(select-keys % [:_id]))))))))

  (facts "GET /api/v1/objectives/:id"
         (fact "gets an objective along with its meta information"
               (let [{username :username :as user} (sh/store-a-user) 
                     stored-objective (sh/store-an-objective {:user user})

                     _ (sh/store-a-comment {:entity stored-objective})
                     _ (sh/store-a-star {:objective stored-objective})

                     objective-uri (str "/objectives/" (:_id stored-objective))
                     {body :body} (-> (p/request app (utils/api-path-for :api/get-objective
                                                                     :id (:_id stored-objective)))
                                      :response)]
                 body => (helpers/json-contains (dissoc stored-objective :global-id :meta))
                 body => (helpers/json-contains {:uri objective-uri})
                 body => (helpers/json-contains {:username username})
                 body => (helpers/json-contains {:meta (contains {:stars-count 1})})
                 body => (helpers/json-contains {:meta (contains {:comments-count 1})})
                 body =not=> (helpers/json-contains {:global-id anything})))

         (fact "returns a 404 if an objective does not exist"
               (p/request app (utils/api-path-for :api/get-objective :id 123456))
               => (contains {:response (contains {:status 404})})))

  (facts "GET /api/v1/objectives/:id?signed-in-id=<user-id>"
         (fact "retrieves the objective by its id, along with meta-information relevant for the signed in user"
               (let [objective-creator (sh/store-a-user)
                     {o-id :_id :as starred-objective} (sh/store-an-objective {:user objective-creator})
                     {user-id :_id :as user} (sh/store-a-user)
                     _ (sh/store-a-star {:user user :objective starred-objective})

                     {body :body} (-> (p/request app (str (utils/api-path-for :api/get-objective :id o-id) "?signed-in-id=" user-id))
                                      :response)
                     retrieved-objective (-> starred-objective
                                             (select-keys [:_id :description :_created_at :created-by-id])
                                             (assoc :username (:username objective-creator))
                                             (assoc :uri (str "/objectives/" o-id)))]
                 body => (helpers/json-contains retrieved-objective)
                 body => (helpers/json-contains {:meta (contains {:starred true})})
                 body => (helpers/json-contains {:meta (contains {:stars-count 1})})
                 body =not=> (helpers/json-contains {:global-id anything}))))

(facts "About retrieving a removed objective"
       (fact "GET /api/v1/objectives/:id?include-removed=true returns the removed objective"
             (let [{username :username :as user} (sh/store-a-user) 
                   stored-objective (sh/store-an-admin-removed-objective {:user user}) 
                   objective-uri (str "/objectives/" (:_id stored-objective))
                   {body :body} (-> (p/request app (str (utils/api-path-for :api/get-objective
                                                                        :id (:_id stored-objective)) 
                                                        "?include-removed=true"))
                                    :response)]
               body => (helpers/json-contains (dissoc stored-objective :global-id :meta))))

       (fact "GET /api/v1/objectives/:id returns a 404"
             (let [{username :username :as user} (sh/store-a-user) 
                   stored-objective (sh/store-an-admin-removed-objective {:user user}) 
                   objective-uri (str "/objectives/" (:_id stored-objective))
                   {status :status} (-> (p/request app (utils/api-path-for :api/get-objective
                                                                       :id (:_id stored-objective)))
                                        :response)]
               status => 404)))

  (facts "about posting objectives"
         (against-background
           (m/valid-credentials? anything anything anything) => true)

         (fact "the posted objective is stored"
               (let [{user-id :_id} (sh/store-a-user)
                     the-objective {:title "my objective title"
                                    :created-by-id user-id}
                     {response :response} (p/request app (utils/api-path-for :api/post-objective)
                                                     :request-method :post
                                                     :content-type "application/json"
                                                     :body (json/generate-string the-objective))]
                 (:body response) => (helpers/json-contains
                                       (assoc the-objective
                                              :uri (contains "/objectives/")))
                 (:body response) =not=> (helpers/json-contains {:global-id anything})
                 (:headers response) => (helpers/location-contains (str "/api/v1/objectives/"))
                 (:status response) => 201))

         (fact "a 400 status is returned if a PSQLException is raised"
               (against-background
                 (objectives/store-objective! anything) =throws=> (org.postgresql.util.PSQLException.
                                                                    (org.postgresql.util.ServerErrorMessage. "" 0)))
               (:response (p/request app (utils/api-path-for :api/post-objective)
                                     :request-method :post
                                     :content-type "application/json"
                                     :body (json/generate-string the-objective))) => (contains {:status 400}))

         (fact "a 400 status is returned if a map->objective exception is raised"
               (:response (p/request app (utils/api-path-for :api/post-objective)
                                     :request-method :post
                                     :content-type "application/json"
                                     :body (json/generate-string the-invalid-objective))) => (contains {:status 400}))))
