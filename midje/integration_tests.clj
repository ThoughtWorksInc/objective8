(ns integration-tests
  (:require [midje.config :refer :all]))

(change-defaults :fact-filter (complement :functional))
