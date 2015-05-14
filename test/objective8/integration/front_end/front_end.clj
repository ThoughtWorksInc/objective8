(ns objective8.integration.front-end.front-end
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.storage.storage :as storage]
            [objective8.handlers.front-end :as front-end]
            [objective8.http-api :as http-api]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.config :as config]
            [objective8.core :as core]))

(def objectives-create-request (mock/request :get "/objectives/create"))
(def objectives-post-request (mock/request :post "/objectives"))

(def default-app (core/app helpers/test-config))

(facts "front end"
       (binding [config/enable-csrf false]
         (fact "google analytics is added to responses"
               (let [{response :response} (p/request (helpers/test-context) (utils/path-for :fe/index))]
                 (:body response)) => (contains "GOOGLE_ANALYTICS_TRACKING_ID")
               (provided
                 (config/get-var "GA_TRACKING_ID") => "GOOGLE_ANALYTICS_TRACKING_ID"))
  
         (facts "authorisation"
                (facts "unauthorised users"
                       (fact "cannot reach the objective creation page"
                             (default-app objectives-create-request) => (contains {:status 302}))
                       (fact "cannot post a new objective"
                             (default-app objectives-post-request) => (contains {:status 302}))
                       (fact "cannot post a comment"
                             (default-app (mock/request :post "/meta/comments")) => (contains {:status 302}))))))
