(ns objective8.unit.middleware-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
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

(def USER_ID 1)
(def OBJECTIVE_ID 3)
(def writer-role (keyword (str "writer-for-" OBJECTIVE_ID)))
(def writer-inviter-role (keyword (str "writer-inviter-for-" OBJECTIVE_ID)))

(facts "about writer-inviter authorisation"
       (fact "a user can access writer-inviter-only resources for an objective they own or are a writer for"
             (let [auth-map (workflows/make-auth {:username USER_ID :roles #{:signed-in writer-inviter-role}})
                   request (friend/merge-authentication {:route-params {:id (str OBJECTIVE_ID)}} auth-map)
                   handler identity
                   wrapped-handler (wrap-authorise-writer-inviter handler)]
               (wrapped-handler request) => (handler request)))
       
       (fact "a user cannot access writer-inviter-only resources for an objective they are not a writer-inviter for"
             (let [auth-map (workflows/make-auth {:username USER_ID :roles #{:signed-in writer-inviter-role}})
                   a-different-objective (inc OBJECTIVE_ID)
                   request (friend/merge-authentication {:route-params {:id (str a-different-objective)}} auth-map)
                   handler identity
                   wrapped-handler (wrap-authorise-writer-inviter handler)]
               (wrapped-handler request) => (throws clojure.lang.ExceptionInfo))))

(facts "about authorisation based on request details"
       (fact "authorises the user against roles specific for the request"
             (let [auth-map (workflows/make-auth {:username USER_ID :roles #{:the-required-role}})
                   request (friend/merge-authentication {:stubbed :request} auth-map)

                   request->roles (fn [request] #{:the-required-role})

                   handler identity
                   wrapped-handler (authorize-based-on-request handler request->roles)]
               (wrapped-handler request) => (handler request))))
