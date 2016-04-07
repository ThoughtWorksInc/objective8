(ns objective8.unit.workflows.okta-test
  (:require [midje.sweet :refer :all]
            [objective8.front-end.workflows.okta :refer :all]
            [objective8.utils :as utils]
            [objective8.front-end.api.http :as http]
            [cheshire.core :as json]))

(def fake-client-id "fake-client-123")
(def fake-client-secret "fake-secret-123")
(def fake-auth-url "https://fake.oktapreview.com")
(def fake-request {:okta-config {:client-id     fake-client-id
                                 :client-secret fake-client-secret
                                 :auth-url      fake-auth-url}})
(defn with-error [request]
  (assoc-in request [:params :error] "access_denied"))

(def token-url (str fake-auth-url "/oauth2/v1/token"))
(def token-code "code-123456789")
(defn with-code [request]
  (assoc-in request [:params :code] token-code))

(def access-token "token-123456789")
(def access-token-response {:body (json/generate-string {:access_token access-token})})

(def user-info-url (str fake-auth-url "/oauth2/v1/userinfo"))
(def okta-user-id "1234567")
(def expiry 9876543210)
(def valid-email "okta@example.com")
(defn user-info-response [& {:keys [expires_at issuer]
                             :or   {expires_at expiry
                                    issuer     fake-auth-url}}]
  {:status 200
   :body   (json/generate-string {:sub   okta-user-id
                                  :exp   expires_at
                                  :email valid-email
                                  :iss   issuer})})
(def code-invalid-error
  {:body (json/generate-string {:error "invalid_grant"})})

(def user-info-error
  {:status  403
   :headers {"WWW-Authenticate" "Bearer error=\"insufficient_scope\", error_description=\"The access token must provide access to at least one of these scopes - profile, email, address or phone\""}})

(fact "step 1: redirect to okta auth page"
      (let [response (okta-sign-in fake-request)]
        response => (contains {:status  302
                               :headers {"Location" (str fake-auth-url "/oauth2/v1/authorize?client_id=" fake-client-id
                                                         "&redirect_uri=" redirect-uri
                                                         "&scope=openid%20email&response_type=code")}})))

;; (fact "step 2: login and permissions acceptance handled by okta")

(facts "step 3: authenticating user"
       (fact "stores the okta user id and email in the session and redirects to sign-up"
             (okta-callback (-> fake-request with-code)) => (contains {:status  302
                                                                       :headers {"Location" (str utils/host-url "/sign-up")}
                                                                       :session {:auth-provider-user-id    (str "okta-" okta-user-id)
                                                                                 :auth-provider-user-email valid-email}})
             (provided
               (http/get-request token-url {:query-params {:grant_type    "authorization_code"
                                                           :client_id     fake-client-id
                                                           :redirect_uri  redirect-uri
                                                           :client_secret fake-client-secret
                                                           :code          token-code}})
               => access-token-response
               (http/get-request user-info-url {:oauth-token access-token})
               => (user-info-response)))
       (fact "redirects to homepage when error in okta"
             (okta-callback (-> fake-request with-error)) => (contains {:status  302
                                                                        :headers {"Location" (str utils/host-url "/error/log-in")}}))
       (fact "redirects to homepage when access token returns an error"
             (okta-callback (-> fake-request with-code)) => (contains {:status  302
                                                                       :headers {"Location" (str utils/host-url "/error/log-in")}})
             (provided
               (http/get-request token-url {:query-params {:grant_type    "authorization_code"
                                                           :client_id     fake-client-id
                                                           :redirect_uri  redirect-uri
                                                           :client_secret fake-client-secret
                                                           :code          token-code}})
               => code-invalid-error))
       (fact "redirects to homepage when user info returns an error"
             (okta-callback (-> fake-request with-code)) => (contains {:status  302
                                                                       :headers {"Location" (str utils/host-url "/error/log-in")}})
             (provided
               (http/get-request token-url {:query-params {:grant_type    "authorization_code"
                                                           :client_id     fake-client-id
                                                           :redirect_uri  redirect-uri
                                                           :client_secret fake-client-secret
                                                           :code          token-code}})
               => access-token-response
               (http/get-request user-info-url {:oauth-token access-token})
               => user-info-error))
       (fact "redirects to homepage when expiry time in user info is before now"
             (okta-callback (-> fake-request with-code)) => (contains {:status  302
                                                                       :headers {"Location" (str utils/host-url "/error/log-in")}})

             (provided
               (http/get-request token-url {:query-params {:grant_type    "authorization_code"
                                                           :client_id     fake-client-id
                                                           :redirect_uri  redirect-uri
                                                           :client_secret fake-client-secret
                                                           :code          token-code}})
               => access-token-response
               (http/get-request user-info-url {:oauth-token access-token})
               => (user-info-response :expires_at 111)))
       (fact "redirects to homepage when the Issuer Identifier in user info doesn't match the expected url"
             (okta-callback (-> fake-request with-code)) => (contains {:status  302
                                                                       :headers {"Location" (str utils/host-url "/error/log-in")}})

             (provided
               (http/get-request token-url {:query-params {:grant_type    "authorization_code"
                                                           :client_id     fake-client-id
                                                           :redirect_uri  redirect-uri
                                                           :client_secret fake-client-secret
                                                           :code          token-code}})
               => access-token-response
               (http/get-request user-info-url {:oauth-token access-token})
               => (user-info-response :issuer "https://evil.oktapreview.com"))))
