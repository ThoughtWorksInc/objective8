(ns objective8.integration.front-end.objectives
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.front-end.api.http :as http-api]
            [objective8.config :as config]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.front-end.permissions :as permissions]
            [hiccup.core :as hiccup]
            [hickory.core :as hickory]
            [hickory.select :as select]))

(def TWITTER_ID "TWITTER_ID")

(def OBJECTIVE_ID 234)
(def USER_ID 1)

(def objectives-create-request (mock/request :get "/objectives/create"))
(def objectives-post-request (mock/request :post "/objectives"))
(def objective-view-get-request (mock/request :get (str "/objectives/" OBJECTIVE_ID)))
(def objective-list-view-get-request (mock/request :get (str "/objectives")))
(def invalid-objective-view-get-request (mock/request :get (str "/objectives/" "not-an-objective-id")))

(def default-app (core/front-end-handler helpers/test-config))

(def user-session (helpers/front-end-context))

(def basic-objective {:title       "my objective title"
                      :_id         OBJECTIVE_ID
                      :description "my objective description"
                      :uri         (str "/objectives/" OBJECTIVE_ID)
                      :meta        {:drafts-count 0 :comments-count 0}})


(facts "objectives"
       (against-background
         ;; Twitter authentication background
         (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
         (http-api/create-user anything) => {:status ::http-api/success
                                             :result {:_id USER_ID}}
         (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                    :result {:_id      USER_ID
                                                                             :username "username"}})
       (binding [config/enable-csrf false]
         (fact "authorised user can post and retrieve objective"
               (against-background (http-api/create-objective
                                     (contains {:title         "my objective title"
                                                :description   "my objective description"
                                                :created-by-id USER_ID})) => {:status ::http-api/success
                                                                              :result {:_id OBJECTIVE_ID}})
               (let [params {:title       "my objective title"
                             :description "my objective description"}
                     form-post (-> user-session
                                   (helpers/with-sign-in "http://localhost:8080/objectives/create")
                                   (p/request "http://localhost:8080/objectives"
                                              :request-method :post
                                              :params params))]
                 (:flash (:response form-post)) => (contains {:type              :share-objective
                                                              :created-objective anything})
                 (-> form-post :response :status) => 302
                 (-> form-post :response :headers) => (helpers/location-contains (str "/objectives/" OBJECTIVE_ID)))))

       (fact "Any user can view an objective"
             (against-background
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result basic-objective}
               (http-api/get-comments anything anything) => {:status ::http-api/success :result {:comments []}}
               (http-api/retrieve-writers OBJECTIVE_ID) => {:status ::http-api/success :result []}
               (http-api/retrieve-questions OBJECTIVE_ID) => {:status ::http-api/success :result []})
             (default-app objective-view-get-request) => (contains {:status 200})
             (default-app objective-view-get-request) => (contains {:body (contains "my objective title")})
             (default-app objective-view-get-request) => (contains {:body (contains "my objective description")}))


       (facts "about promoting objectives"
              (fact "the promote objective button is hidden by default"
                    (against-background
                      (http-api/get-objectives) => {:status ::http-api/success
                                                    :result [{:_id 1 :description "description" :title "title"}]})
                    (default-app objective-list-view-get-request) =not=> (contains {:body (contains "/objectives/promote-objective")}))

              (fact "the promote objective button posts to the correct endpoint"
                    (-> user-session
                        helpers/sign-in-as-existing-user
                        (p/request "http://localhost:8080/objectives")
                        :response)
                    => (contains {:body (contains "/meta/promote-objective")})
                    (provided (http-api/get-objectives {:signed-in-id 1}) => {:status ::http-api/success
                                                                              :result [{:_id 1 :description "description" :title "title"}]}
                              (permissions/admin? {:username "username", :roles #{:signed-in}, :email nil}) => true))


              (fact "only one promote objectives button appears"
                    (-> user-session
                        helpers/sign-in-as-existing-user
                        (p/request "http://localhost:8080/objectives")
                        :response
                        :body
                        ((partial re-seq (re-pattern "clj-promote-objective-form-container")))
                        count)
                    => 1
                    (provided (http-api/get-objectives {:signed-in-id 1}) => {:status ::http-api/success
                                                                              :result [{:_id 1 :description "description" :title "title"}]}
                              (permissions/admin? {:username "username", :roles #{:signed-in}, :email nil}) => true))

              (fact "promoted objectives container is populated with objectives"
                    (against-background
                      (http-api/get-objectives) => {:status ::http-api/success
                                                    :result [{:_id 1 :description "description" :title "title" :promoted true}]})
                    (default-app objective-list-view-get-request) => (contains {:body (contains "clj-promoted-objectives-container")}))

              (fact "promoted objectives container is populated only with promoted objectives"
                    (against-background
                      (http-api/get-objectives) => {:status ::http-api/success
                                                    :result [basic-objective {:_id 1 :description "promoted description" :title "promoted title" :promoted true}]})

                    (let [response-tree (-> (default-app objective-list-view-get-request) :body hickory/parse hickory/as-hickory)]

                      (str (select/select (select/class "clj-promoted-objective-list") response-tree)) => (contains "promoted description")
                      (str (select/select (select/class "clj-promoted-objective-list") response-tree)) =not=> (contains "my objective description")

                      (str (select/select (select/class "clj-objective-list") response-tree)) =not=> (contains "promoted description")
                      (str (select/select (select/class "clj-objective-list") response-tree)) => (contains "my objective description")
                      )
                    )

              (fact "promoted objectives container is hidden if there are no promoted objectives"
                    (against-background
                      (http-api/get-objectives) => {:status ::http-api/success
                                                    :result [{:_id 1 :description "description" :title "title"}]})
                    (default-app objective-list-view-get-request) =not=> (contains {:body (contains "clj-promoted-objectives-container")})
                    )

              (fact "promoted objectives button becomes 'Demote' for promoted objectives"
                    (against-background
                      (http-api/get-objectives) => {:status ::http-api/success
                                                    :result [{:_id 1 :description "promoted description" :title "promoted title" :promoted true}]}
                      (permissions/admin? {:email nil}) => true)
                    (default-app objective-list-view-get-request) => (contains {:body (contains "demote-text")})
                    (default-app objective-list-view-get-request) =not=> (contains {:body (contains "promote-text")}))

              (fact "promote objective button is hidden if there are already 3 promoted objectives"
                    (against-background
                      (http-api/get-objectives) => {:status ::http-api/success
                                                    :result [{:_id 1 :description "first promoted description" :title "first promoted title" :promoted true}
                                                             {:_id 2 :description "second promoted description" :title "second promoted title" :promoted true}
                                                             {:_id 3 :description "third promoted description" :title "third promoted title" :promoted true}
                                                             basic-objective
                                                             ]}
                      (permissions/admin? {:email nil}) => true)

                    (default-app objective-list-view-get-request) =not=> (contains {:body (contains "promote-text")})
                    (default-app objective-list-view-get-request) => (contains {:body (contains "demote-text")})
                    )

              (facts "About promoted objective information"
                     (against-background
                       (http-api/get-objectives) => {:status ::http-api/success
                                                     :result [{:_id 1 :description "promoted description" :title "promoted title" :promoted true}]})

                     (fact "Promoted objective information does not appear for normal users"
                           (default-app objective-list-view-get-request) =not=> (contains {:body (contains "clj-promoted-objective-information")}))

                     (fact "Promoted objective information appears for admin users"
                           (against-background (permissions/admin? {:email nil}) => true)
                           (default-app objective-list-view-get-request) => (contains {:body (contains "clj-promoted-objective-information")})
                           )))

       (fact "only one remove objective button appears"
             (-> user-session
                 helpers/sign-in-as-existing-user
                 (p/request "http://localhost:8080/objectives")
                 :response
                 :body
                 ((partial re-seq (re-pattern "clj-objective-list-item-removal-container")))
                 count)
             => 1
             (provided (http-api/get-objectives {:signed-in-id 1}) => {:status ::http-api/success
                                                                       :result [{:_id 1 :description "description" :title "title"}]}
                       (permissions/admin? {:username "username", :roles #{:signed-in}, :email nil}) => true))

       (fact "When a signed in user views an objective, the objective contains user specific information"
             (against-background
               (http-api/get-comments anything anything) => {:status ::http-api/success :result {:comments []}}
               (http-api/retrieve-writers OBJECTIVE_ID) => {:status ::http-api/success :result []}
               (http-api/retrieve-questions OBJECTIVE_ID) => {:status ::http-api/success :result []})
             (-> user-session
                 helpers/sign-in-as-existing-user
                 (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID))
                 :response) => (contains {:body (contains "clj-username")})
             (provided
               (http-api/get-objective OBJECTIVE_ID {:signed-in-id USER_ID}) => {:status ::http-api/success
                                                                                 :result basic-objective}))

       (fact "A user should receive a 404 if an objective doesn't exist"
             (against-background
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/not-found})
             (default-app objective-view-get-request) => (contains {:status 404}))

       (facts "about comments"
              (against-background
                (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                          :result basic-objective}
                (http-api/retrieve-writers OBJECTIVE_ID) => {:status ::http-api/success :result []}
                (http-api/retrieve-questions OBJECTIVE_ID) => {:status ::http-api/success :result []})

              (fact "Any user can view comments with votes on an objective"
                    (against-background
                      (http-api/get-comments anything anything) => {:status ::http-api/success
                                                                    :result {:comments [{:_id           1
                                                                                         :_created_at   "2015-02-12T16:46:18.838Z"
                                                                                         :objective-id  OBJECTIVE_ID
                                                                                         :created-by-id USER_ID
                                                                                         :comment       "Comment 1"
                                                                                         :uri           "/comments/1"
                                                                                         :votes         {:up 123456789 :down 987654321}}]}})
                    (let [{response :response} (p/request user-session (str "http://localhost:8080/objectives/" OBJECTIVE_ID))]
                      (:body response) => (contains "Comment 1")
                      (:body response) => (contains "123456789")
                      (:body response) => (contains "987654321"))))

       (fact "A user should see an error page when they attempt to access an objective with a non-integer ID"
             (default-app invalid-objective-view-get-request) => (contains {:status 404})))

(facts "About viewing the list of objectives"
       (against-background
         ;; Twitter authentication background
         (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
         (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                    :result {:_id      USER_ID
                                                                             :username "username"}})

       (fact "when the viewing user is not signed in, the get-objectives query is made with no user information"
             (p/request user-session "http://localhost:8080/objectives") => anything
             (provided
               (http-api/get-objectives) => {:status ::http-api/success
                                             :result []}))

       (fact "when the viewing user is signed in, the get-objectives query is made for that user"
             (-> user-session
                 helpers/sign-in-as-existing-user
                 (p/request "http://localhost:8080/objectives")) => anything
             (provided
               (http-api/get-objectives {:signed-in-id USER_ID}) => {:status ::http-api/success
                                                                     :result []})))
