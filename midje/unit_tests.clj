(ns unit-tests
  (:require [midje.config :refer :all]))

(defn not-integration-or-functional [fact-meta-data]
  (not-any? #(contains? fact-meta-data %) [:integration :functional]))

(change-defaults :fact-filter not-integration-or-functional)
