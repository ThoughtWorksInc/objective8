(ns unit-tests
  (:require [midje.config :refer :all]))

(defn not-integration-or-functional [fact-meta-data]
  (not (or (contains? fact-meta-data :integration) (contains? fact-meta-data :functional))))

(change-defaults :fact-filter not-integration-or-functional)
