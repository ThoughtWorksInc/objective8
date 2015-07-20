(ns dev-helpers.stub-twitter
  (:require [bidi.ring :refer [make-handler]]
            [clojure.tools.logging :as log]
            [ring.util.response :as response]
            [objective8.front-end.workflows.sign-up :refer [sign-up-workflow authorise]]
            [objective8.front-end.api.http :as http-api]
            [objective8.utils :as utils]))

;; Stub out twitter authentication workflow

(def twitter-id (atom "twitter-FAKE_ID"))

(defn stub-twitter-handler [request]
  (let [session (:session request)]
    (log/info "Stubbing twitter with fake twitter id: " @twitter-id)
    (-> (response/redirect (str utils/host-url "/sign-up"))
        (assoc :session (assoc session 
                               :auth-provider-user-id @twitter-id
                               :twitter-screen-name "I'm a teapot")))))

(defn create-or-sign-in [{params :params :as request}]
  (let [user-id (:user-id params)
        user-map {:twitter-id (str "twitter-load-test-" user-id)
                  :username (str "username-load-test-" user-id)
                  :email-address (str "email-" user-id "@loadtest.com")}
        {status :status user :result :as find-result} (http-api/find-user-by-auth-provider-user-id (:twitter-id user-map))]
    (if (= status ::http-api/success)
      (authorise {:status 200} user)

      (let [{status :status user :result} (http-api/create-user user-map)]
        (if (= status ::http-api/success)
          (authorise {:status 200} user)

          (do (prn (str "Creating user failed: " status
                        "\n params: " params
                        "\n session: " (:session request)))
              {:status 500}))))))

(def stub-twitter-workflow
  (make-handler ["/" {"create-or-sign-in" create-or-sign-in   
                      "twitter-sign-in" stub-twitter-handler}]))

(def stub-twitter-auth-config
  {:allow-anon? true
   :workflows [stub-twitter-workflow
               sign-up-workflow]
   :login-uri "/sign-in"})

