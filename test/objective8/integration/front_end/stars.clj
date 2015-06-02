(ns objective8.integration.front-end.stars
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.front-end.api.http :as http-api]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as helpers]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)
(def OBJECTIVE_URI (str "/objectives/" OBJECTIVE_ID))
(def STAR_ID 2)

(facts "about stars"
       (binding [config/enable-csrf false]
         (fact "authorised user can post a star on an objective"
               (against-background
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success})
               (against-background
                 (http-api/post-star {:objective-uri OBJECTIVE_URI
                                      :created-by-id USER_ID}) => {:status ::http-api/success
                                                                   :result {:_id STAR_ID
                                                                            :objective-id OBJECTIVE_ID
                                                                            :created-by-id USER_ID
                                                                            :active true}})
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id USER_ID}
                 (http-api/create-user anything) => {:status ::http-api/success
                                                     :result {:_id USER_ID}})
               (let [user-session (helpers/front-end-context)
                     params {:objective-uri OBJECTIVE_URI
                             :refer OBJECTIVE_URI}
                     {response :response} (-> user-session
                                              (helpers/with-sign-in (str "http://localhost:8080/objectives/" OBJECTIVE_ID))
                                              (p/request (utils/path-for :fe/post-star)
                                                         :request-method :post
                                                         :params params))]
                 (:headers response) => (helpers/location-contains OBJECTIVE_URI)
                 (:status response) => 302))))
