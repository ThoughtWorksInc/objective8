(ns objective8.middleware-test
  (:require [midje.sweet :refer :all]
            [objective8.middleware :refer :all]))

(def request {:headers {"api-bearer-token" "some-secure-token"
                        "api-bearer-name"  "objective8.dev"
                        "some-other-header" "friday"}})

(fact "Calls the handler and dissocs details (so they don't get logged) if the bearer token is correct"
      (let [handler identity 
            wbt-wrapped-handler (wrap-bearer-token handler {:objective8.dev "some-secure-token"})]
        (wbt-wrapped-handler request) => {:headers {"some-other-header" "friday"}}))

(fact "Returns 401 Unauthorized if the bearer token is incorrect"
      (let [handler identity 
            wbt-wrapped-handler (wrap-bearer-token handler {:objective8.dev "some-secure-token"})]
        (wbt-wrapped-handler (assoc-in request [:headers "api-bearer-token"] "wibble")) => (contains {:status 401})))

(fact "Returns 401 Unauthorized if the bearer name does not exist"
      (let [handler identity 
            wbt-wrapped-handler (wrap-bearer-token handler {:objective8.dev "some-secure-token"})]
        (wbt-wrapped-handler (assoc-in request [:headers "api-bearer-name"] "wibble")) => (contains {:status 401})))

(fact "Returns 401 Unauthorized if the user and bearer token do not match"
      (let [handler identity 
            wbt-wrapped-handler (wrap-bearer-token handler {:objective8.dev "some-secure-token"
                                                            :someone-else "12345"})]
        (wbt-wrapped-handler (assoc-in request [:headers "api-bearer-name"] "someone-else")) => (contains {:status 401})))
