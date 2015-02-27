(ns objective8.workflows.sign-up-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [objective8.workflows.sign-up :refer :all]
            [objective8.utils :as utils]))

(fact "retain invitation information in session when signing in / signing up with redirect"
      (let [session-with-invitation {:invitation :invitation-data}]
        (authorised-redirect :user :some-url session-with-invitation)) => :authorised-response
        (provided
         (authorise (contains {:session (contains {:invitation :invitation-data})}) :user) => :authorised-response))
