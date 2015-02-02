(ns d-cent.integration-helpers
  (:require [d-cent.core :as core]
            [peridot.core :as p]))

(defn with-sign-in [user-session url & args]
  (let [request-function #(apply p/request % url args)]
    (-> user-session
        ; Hit unauthorized url
        request-function
        ; Retrieve twitter user-name (NB: requires twitter authentication background to be set)
        (p/request "http://localhost:8080/twitter-callback?oauth_verifier=the-verifier")
        ; Follow redirect to capture-profile-workflow
        (p/follow-redirect)
        ; Follow redirect to originally requested resource
        (p/follow-redirect))))

(defn test-context
  "Creates a fake application context which uses the provided atom
   as database storage"

  ([]
   (p/session (core/app core/app-config)))

  ([test-store]
   (p/session (core/app (assoc core/app-config :store test-store)))))
