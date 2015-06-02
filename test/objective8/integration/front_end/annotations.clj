(ns objective8.integration.front-end.annotations 
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.front-end.api.http :as http-api]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.core :as core]))

(def USER_ID 1)
(def COMMENT_ID 123)
(def OBJECTIVE_ID 234)
(def GLOBAL_ID 223)
(def OBJECTIVE_URI (str "/objectives/" OBJECTIVE_ID))

(def DRAFT_ID 56)
(def SECTION_LABEL "1234abcd")
(def SECTION_URI (str "/objectives/" OBJECTIVE_ID "/drafts/" DRAFT_ID "/sections/" SECTION_LABEL))
(def user-session (helpers/front-end-context))

(facts "annotations"
       (binding [config/enable-csrf false]
         (fact "authorised user can post an annotation against a draft"  
              (against-background
               (http-api/post-comment {:comment "The comment"
                                       :comment-on-uri SECTION_URI 
                                       :created-by-id USER_ID
                                       :reason "expand"}) => {:status ::http-api/success
                                                                    :result  {:_id 12
                                                                              :created-by-id USER_ID
                                                                              :comment-on-uri SECTION_URI 
                                                                              :comment "The comment"
                                                                              :reason "expand"}})
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id USER_ID}
                 (http-api/create-user anything) => {:status ::http-api/success
                                                     :result {:_id USER_ID}})
               (let [params {:comment "The comment"
                             :refer SECTION_URI
                             :comment-on-uri SECTION_URI 
                             :reason "expand"}
                     {response :response} (-> user-session
                                              (helpers/with-sign-in (str "http://localhost:8080/objectives/" OBJECTIVE_ID))
                                              (p/request (utils/path-for :fe/post-annotation :id OBJECTIVE_ID
                                                                         :d-id DRAFT_ID
                                                                         :section-label SECTION_LABEL)
                                                         :request-method :post
                                                         :params params))]
                 (:flash response) => (contains {:type :flash-message :message :draft-section/annotation-created-message})
                 (:headers response) => (helpers/location-contains SECTION_URI)
                 (:status response) => 302))))
