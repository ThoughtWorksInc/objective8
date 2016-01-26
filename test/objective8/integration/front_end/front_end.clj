(ns objective8.integration.front-end.front-end
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [peridot.core :as p]
            [objective8.integration.integration-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.config :as config]
            [objective8.core :as core]))

(def objectives-create-request (mock/request :get "/objectives/create"))
(def objectives-post-request (mock/request :post "/objectives"))

(def default-app (core/front-end-handler helpers/test-config))

(facts "front end"
       (binding [config/enable-csrf false]
         (fact "google analytics is added to responses"
               (binding [config/environment (assoc config/environment
                                                   :google-analytics-tracking-id "GOOGLE_ANALYTICS_TRACKING_ID")]
                 (let [{response :response} (p/request (helpers/front-end-context) (utils/path-for :fe/index))]
                   (:body response))) => (contains "GOOGLE_ANALYTICS_TRACKING_ID"))

         (facts "authorisation"
                (facts "unauthorised users"
                       (fact "cannot reach the objective creation page"
                             (default-app objectives-create-request) => (contains {:status 302}))
                       (fact "cannot post a new objective"
                             (default-app objectives-post-request) => (contains {:status 302}))
                       (fact "cannot post a comment"
                             (default-app (mock/request :post "/meta/comments")) => (contains {:status 302}))))))

(facts "about white-labelling"
       (fact "defaults are used when the env var is not set"
             (let [{default-response :response} (p/request (helpers/front-end-context) (utils/path-for :fe/index))]
               (:body default-response) => (every-checker (contains "/static/favicon.ico")
                                                          (contains "Objective[8]"))))
       (fact "custom favicon is used when env var is set"
             (binding [config/environment (assoc config/environment
                                            :favicon-file-name "custom.ico")]
               (let [{response :response} (p/request (helpers/front-end-context) (utils/path-for :fe/index))]
                 (:body response) => (contains "/static/custom.ico"))))
       (fact "custom app name is used when env var is set"
             (binding [config/environment (assoc config/environment
                                            :app-name "Policy Maker")]
               (let [{response :response} (p/request (helpers/front-end-context) (utils/path-for :fe/index))]
                 (:body response) => (every-checker
                                       (contains "<a href=\"/\" title=\"Go to home page\" rel=\"home\" data-l8n=\"attr/title:masthead/logo-title-attr\" class=\"masthead-logo\">Policy Maker</a>"))))))

(facts "about the alpha warnings"
       (fact "it is hidden when env var is falsey"
             (binding [config/environment (assoc config/environment :show-alpha-warnings false)]
               (let [{response :response} (p/request (helpers/front-end-context) (utils/path-for :fe/index))]
                 (:body response) =not=> (contains "\"status-bar clj-status-bar\"")
                 (:body response) =not=> (contains "\"footer-content-text footer-alpha-warning\""))))
       (fact "it is shown when env var is truthy"
             (binding [config/environment (assoc config/environment :show-alpha-warnings true)]
               (let [{response :response} (p/request (helpers/front-end-context) (utils/path-for :fe/index))]
                 (:body response) => (contains "\"status-bar clj-status-bar\"")
                 (:body response) => (contains "\"footer-content-text footer-alpha-warning\"")))))