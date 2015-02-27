(ns dev-helpers.stub-twitter
  (:require [bidi.ring :refer [make-handler]]
            [ring.util.response :as response]
            [objective8.workflows.sign-up :refer [sign-up-workflow]]
            [objective8.utils :as utils]))

;; Stub out twitter authentication workflow

(defn stub-twitter-handler [request]
  (let [session (:session request)]
    (-> (response/redirect (str utils/host-url "/sign-up"))
        (assoc :session (assoc session 
                               :twitter-id "twitter-FAKE_ID"
                               :twitter-screen-name "I'm a teapot")))))

(def stub-twitter-workflow
  (make-handler ["/twitter-sign-in" stub-twitter-handler]))

(def stub-twitter-auth-config
  {:allow-anon? true
   :workflows [stub-twitter-workflow
               sign-up-workflow]
   :login-uri "/sign-in"})

