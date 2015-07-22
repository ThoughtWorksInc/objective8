(ns objective8.unit.workflows.stonecutter-test
  (:require [midje.sweet :refer :all]
            [stonecutter-oauth.client :as soc]
            [objective8.utils :as utils]
            [objective8.front-end.workflows.stonecutter :refer :all]))

(fact "stonecutter-sign-in generates the correct response"
      (stonecutter-sign-in {:stonecutter-config ...config...}) => ...stonecutter-sign-in-response...
      (provided
        (soc/authorisation-redirect-response ...config...) => ...stonecutter-sign-in-response...))

(fact "stonecutter-callback redirects to the sign-in workflow with the auth-provider-user-id set in the session"
      (stonecutter-callback {:stonecutter-config ...config... :params {:code ...auth-code...}})
      => (contains {:status 302
                    :headers {"Location" (str utils/host-url "/sign-up")}
                    :session {:auth-provider-user-id "d-cent-USER_ID"}})
      (provided
        (soc/request-access-token! ...config... ...auth-code...)
        => {:status :success
            :token {:user-id "USER_ID"}}))

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
