(ns objective8.integration.front-end.users 
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.handlers.front-end :as front-end]
            [objective8.http-api :as http-api]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as helpers]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)
(def OBJECTIVE_URI (str "/objectives/" OBJECTIVE_ID))
(def STAR_ID 2)

(facts "about viewing profile page"
       (fact "profile is shown of the user matching the provided username"
             (let [user-session (helpers/test-context)
                   {response :response} (-> user-session
                                            (p/request (utils/path-for :fe/profile 
                                                                       :username "username")))]
               (:status response)) => 200
             (provided
               (http-api/find-user-by-username "username") => {:status ::http-api/success
                                                               :result {:username "username"
                                                                        :profile {:name "Barry"
                                                                                  :biog "I'm Barry..."}}}))) 
