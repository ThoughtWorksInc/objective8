(ns objective8.middleware-test
  (:require [midje.sweet :refer :all]
            [objective8.middleware :refer :all]))

(def request {:headers {"api-bearer-token" "some-secure-token"
                        "api-bearer-name"  "objective8.dev"
                        "some-other-header" "friday"}})

(defn test-token-provider [name]
  (throw (Exception. "This is only for use in provided statements")))

(fact "Calls the handler and dissocs details (so they don't get logged) if the provided bearer token is correct"
      (let [handler identity 
            wbt-wrapped-handler (wrap-bearer-token handler test-token-provider)]
        (wbt-wrapped-handler request)) => {:headers {"some-other-header" "friday"}} 
      (provided (test-token-provider "objective8.dev") => "some-secure-token"))

(fact "Returns 401 Unauthorized if the bearer token does not match provided bearer token"
      (let [handler identity 
            wbt-wrapped-handler (wrap-bearer-token handler test-token-provider)]
        (wbt-wrapped-handler (assoc-in request [:headers "api-bearer-token"] "wibble"))) => (contains {:status 401})
      (provided (test-token-provider "objective8.dev") => "some-secure-token"))

(fact "Returns 401 Unauthorized if the bearer name does not exist in the headers"
      (let [handler identity 
            wbt-wrapped-handler (wrap-bearer-token handler test-token-provider)]
        (wbt-wrapped-handler (update-in request [:headers] dissoc "api-bearer-name")) => (contains {:status 401})))

(fact "Returns 401 Unauthorized if the token-provider returns nil"
      (let [handler identity 
            wbt-wrapped-handler (wrap-bearer-token handler test-token-provider)]
        (wbt-wrapped-handler request)) => (contains {:status 401})
      (provided (test-token-provider "objective8.dev") => nil))

(fact "Returns 401 Unauthorized if the user and bearer token do not match"
      (let [handler identity 
            wbt-wrapped-handler (wrap-bearer-token handler {:objective8.dev "some-secure-token"
                                                            :someone-else "12345"})]
        (wbt-wrapped-handler (assoc-in request [:headers "api-bearer-name"] "someone-else")) => (contains {:status 401})))

(tabular

 (fact "Removes trailing slashes from non-root URIs"
       (let [handler identity
             sts-wrapped-handler (strip-trailing-slashes handler)]
         (sts-wrapped-handler {:uri ?original-uri})) => {:uri ?stripped-uri})

 ?original-uri   ?stripped-uri
 "/"            "/"
 "something/"   "something"
 "something"    "something"
 "a/b/"         "a/b")
