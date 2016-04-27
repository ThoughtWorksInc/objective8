(ns objective8.integration.front-end.sign-in
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.config :as config]
            [objective8.core :as core]
            [ring.mock.request :as mock]))

(def okta-credentials {:client-id     "client-123"
                       :client-secret "secret-456"
                       :auth-url      "https://okta-instance.com"})

(def default-app (core/front-end-handler helpers/test-config))
(def private-mode-app (core/front-end-handler helpers/test-config))

(defn mock-get-request [path]
  (mock/request :get path))

(facts "Private mode"
       (fact "When disabled, endpoints should not be redirected"
             (binding [config/environment (assoc config/environment :private-mode-enabled false)]

               (p/request (helpers/front-end-context) utils/host-url) =not=> (helpers/check-redirects-to "/sign-in" 302)))

       (fact "when enabled, endpoints should be redirected to sign in"
             (binding [config/environment (assoc config/environment :private-mode-enabled true)]

               (p/request (helpers/front-end-context) utils/host-url) => (helpers/check-redirects-to "/sign-in" 302)))

       (tabular
         (fact "when Okta credentials provided, endpoints should be redirected to sign in"
               ;TODO - work out how to reconfigure friend to have a different auth url. No Idea where the authenticate middleware is configured.
               (binding [config/environment (assoc config/environment :okta-credentials okta-credentials :private-mode-enabled ?private)]

                 (p/request (helpers/front-end-context) utils/host-url) => (helpers/check-redirects-to "/sign-in" 302)))
         ?private
         true
         false))