(ns objective8.unit.workflows.facebook-test
  (:require [midje.sweet :refer :all]
            [objective8.front-end.workflows.facebook :refer :all]
            [cheshire.core :as json]
            [objective8.front-end.api.http :as http]
            [objective8.utils :as utils]))

(def fake-client-id "o8-id")
(def fake-client-secret "shh")
(def fake-request {:facebook-config {:client-id     fake-client-id
                                     :client-secret fake-client-secret}})
(def login-code "fb-code")
(def access-token "fb-token")
(def access-token-response {:body (json/generate-string {:access_token access-token})})
(def user-id "facebook-1234567")
(def token-info-response {:body (json/generate-string {:data {:user-id 1234567}})})

(defn with-code [request]
  (assoc-in request [:params :code] login-code))

(fact "step 1: redirect to facebook auth page"
      (let [response (facebook-sign-in fake-request)]
        response => (contains {:status  302
                               :headers {"Location" (str "https://www.facebook.com/dialog/oauth?client_id=" fake-client-id
                                                         "&redirect_uri=" redirect-uri)}})))

;; (fact "step 2: login and permissions acceptance handled by facebook")

(facts "step 3: authenticating user"
       (fact "stores the facebook user id in the session and redirects to sign-up"
             (facebook-callback (-> fake-request with-code)) => (contains {:status  302
                                                                           :headers {"Location" (str utils/host-url "/sign-up")}
                                                                           :session {:auth-provider-user-id user-id}})

             (provided
               (http/get-request "https://graph.facebook.com/v2.3/oauth/access_token" {:query-params {:client_id     fake-client-id
                                                                                                      :redirect_uri  redirect-uri
                                                                                                      :client_secret fake-client-secret
                                                                                                      :code          login-code}})
               => access-token-response
               (http/get-request "https://graph.facebook.com/debug_token" {:query-params {:input_token  access-token
                                                                                          :access_token (str fake-client-id "|" fake-client-secret)}})
               => token-info-response)))