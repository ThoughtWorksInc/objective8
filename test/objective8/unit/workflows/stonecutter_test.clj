(ns objective8.unit.workflows.stonecutter-test
  (:require [midje.sweet :refer :all]
            [stonecutter-oauth.client :as soc]
            [stonecutter-oauth.jwt :as so-jwt]
            [objective8.utils :as utils]
            [objective8.front-end.workflows.stonecutter :refer :all]))

(def openid-test-config (soc/configure "ISSUER" "CLIENT_ID" "<client-secret>" "<callback-uri>"
                                       :protocol :openid))

(def test-auth-provider-public-key (slurp "./test/objective8/unit/workflows/test-stonecutter-key.json"))
(def token-expiring-in-year-2515 "eyJraWQiOiJ0ZXN0LWtleSIsImFsZyI6IlJTMjU2In0.eyJpc3MiOiJJU1NVRVIiLCJhdWQiOiJDTElFTlRfSUQiLCJleHAiOjE3MjA3OTkzMjUyLCJpYXQiOjE0Mzk5OTI3NDAsInN1YiI6IlNVQkpFQ1QiLCJyb2xlIjoiYWRtaW4iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiZW1haWwiOiJlbWFpbEBhZGRyZXNzLmNvbSJ9.PQWWJQGECzC8EchkfwGjQBBUfhFGoLDOjZ1Ohl1t-eo8rXDO4FxONk3rYEY9v01fVg3pzQW8zLJYcZ73gyE2ju8feHhwS8wYwcsgKq6XC-Zr9LwRJIeFpZoVcgMpvW21UHX1bxAhHE7WM_UzSerKtGkIuK21XraGVTiIB-0o8eWOJX0Rud8FXC3Cr0LdZeqDytPZDwM1Pbcr0eFyfNq9ngi75BFNTGHCMLGshJGt1LvQhDtTWifXDlwW5uk-kuOVavnQGK_i7qvrcy8c7lFCCPqd5X3x6EZJyfk-BZGgDT1ySwdM2EjRAi1W1nPAmdWms9rts0rkbk_Q73gEkWQpOw")

;token-content is the decoded version of token-expiring-in-year-2515 signed with test-auth-provider-public-key
(def token-content {:aud "CLIENT_ID"
                    :email "email@address.com"
                    :email_verified true
                    :exp 17207993252
                    :iat 1439992740
                    :iss "ISSUER"
                    :role "admin"
                    :sub "SUBJECT"})

(fact "stonecutter-sign-in generates the correct response"
      (stonecutter-sign-in {:stonecutter-config openid-test-config}) => ...stonecutter-sign-in-response...
      (provided
        (soc/authorisation-redirect-response openid-test-config) => ...stonecutter-sign-in-response...))

(facts "about stonecutter-callback"
       (fact "stonecutter-callback redirects to the sign-in workflow with the auth-provider-user-id and auth-provider-user-email set in the session"
             (stonecutter-callback {:stonecutter-config openid-test-config :params {:code ...auth-code...} :session {:sign-in-referrer ...refer...}})
             => (contains {:status 302
                           :headers {"Location" (str utils/host-url "/sign-up")}
                           :session {:auth-provider-user-id "d-cent-SUBJECT"
                                     :auth-provider-user-email "email@address.com"
                                     :sign-in-referrer ...refer...}})
             (provided
               (soc/request-access-token! openid-test-config ...auth-code...)
               => {:id_token token-expiring-in-year-2515}
               (so-jwt/get-public-key-string-from-jwk-set-url "ISSUER/api/jwk-set")
               => test-auth-provider-public-key))

       (fact "redirects to invalid-configuration page when unable to retrieve token from auth server"
             (against-background
               (soc/request-access-token! anything anything) =throws=> (Exception. "Some exception"))

             (get-in (stonecutter-callback {:stonecutter-config ...config... :params {:code ...auth-code...}})
                     [:status]) => 302
             (get-in (stonecutter-callback {:stonecutter-config ...config... :params {:code ...auth-code...}})
                     [:headers "Location"]) => (contains (utils/path-for :fe/error-configuration))))

(facts "about wrap-stonecutter-config"
       (fact "includes the stonecutter configuration in the request when the configuration is valid"
             (let [handler (wrap-stonecutter-config identity ...valid-stonecutter-config... ...invalid-handler...)]
               (handler {}) => {:stonecutter-config ...valid-stonecutter-config...}))

       (fact "defaults to invalid-handler when configuration is invalid"
             (wrap-stonecutter-config identity :invalid-configuration ...invalid-handler...) => ...invalid-handler...))

(fact "invalid-handler redirects to the invalid configuration error page"
      (invalid-configuration-handler {})
      => (contains {:status 302
                    :headers (contains {"Location" (contains "/error/configuration")})}))
