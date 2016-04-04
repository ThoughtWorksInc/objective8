(ns objective8.unit.workflows.okta-test
  (:require [midje.sweet :refer :all]
            [objective8.front-end.workflows.okta :refer :all]
            [objective8.utils :as utils]
            [objective8.front-end.api.http :as http]
            [cheshire.core :as json]))

(def fake-client-id "fake-client-123")
(def fake-client-secret "fake-secret-123")
(def fake-request {:okta-config {:client-id     fake-client-id
                                 :client-secret fake-client-secret}})

(def token-code "code-123456789")
(defn with-code [request]
  (assoc-in request [:params :code] token-code))

(def access-token "token-123456789")
(def access-token-response {:body (json/generate-string {:access_token access-token})})

(def okta-user-id "1234567")
(def expiry 9876543210)
(def valid-email "okta@example.com")
(def user-info-response
  {:body (json/generate-string {:sub   okta-user-id
                                :exp   expiry
                                :email valid-email})})

(fact "step 1: redirect to facebook auth page"
      (let [response (okta-sign-in fake-request)]
        response => (contains {:status  302
                               :headers {"Location" (str "https://thoughtworks.oktapreview.com/oauth2/v1/authorize?client_id=" fake-client-id
                                                         "&redirect_uri=" redirect-uri
                                                         "&scope=email&response_type=code")}})))

;; (fact "step 2: login and permissions acceptance handled by okta")

(facts "step 3: authenticating user"
       (fact "stores the facebook user id and email in the session and redirects to sign-up"
             (okta-callback (-> fake-request with-code)) => (contains {:status  302
                                                                       :headers {"Location" (str utils/host-url "/sign-up")}
                                                                       :session {:auth-provider-user-id    (str "okta-" okta-user-id)
                                                                                 :auth-provider-user-email valid-email}})
             (provided
               (http/get-request "https://thoughtworks.oktapreview.com/oauth2/v1/token" {:query-params {:grant_type    "authorization_code"
                                                                                                        :client_id     fake-client-id
                                                                                                        :redirect_uri  redirect-uri
                                                                                                        :client_secret fake-client-secret
                                                                                                        :code          token-code}})
               => access-token-response
               (http/get-request "https://thoughtworks.oktapreview.com/oauth2/v1/userinfo" {:headers {:oauth-token access-token}})
               => user-info-response)))