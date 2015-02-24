(ns objective8.front-end.authors-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.config :as config] 
            [objective8.integration-helpers :as helpers]
            [objective8.http-api :as http-api]))

(def USER_ID 1)
(def OBJECTIVE_ID 2)
(def INVITATION_ID 3)

(facts "about authors" :integration
       (binding [config/enable-csrf false]
         (fact "authorised user can suggest an author on an objective"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id USER_ID}
                 (http-api/create-user anything) => {:status ::http-api/success
                                                     :result {:_id USER_ID}}
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success}
                 (http-api/suggest-author {:author-name "bob"
                                           :reason "he's awesome"
                                           :objective-id OBJECTIVE_ID
                                           :suggested-by-id USER_ID}) => {:status ::http-api/success
                                                                          :result {:_id INVITATION_ID
                                                                                   :objective-id OBJECTIVE_ID}})
               (let [user-session (helpers/test-context)
                     params {:author-name "bob"
                             :reason "he's awesome"}
                     peridot-response (-> user-session
                                          (helpers/with-sign-in "http://localhost:8080/")
                                          (p/request (str "http://localhost:8080/objectives/" OBJECTIVE_ID "/authors")
                                                     :request-method :post
                                                     :params params))]
                 peridot-response => (helpers/flash-message-contains "Your suggested author has been added!")
                 peridot-response => (helpers/headers-location (str "/objectives/" OBJECTIVE_ID "/authors"))))))
