(ns d-cent.integration-helpers
  (:require [d-cent.core :as core]
            [peridot.core :as p]))

(defn test-context
  "Creates a fake application context which uses the provided atom
   as database storage"
  [test-store]
  (p/session (core/app (assoc core/app-config :store test-store))))
