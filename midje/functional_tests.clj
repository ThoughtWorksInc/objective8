(ns functional-tests
  (:require [midje.config :refer :all]))

(change-defaults :fact-filter :functional)
