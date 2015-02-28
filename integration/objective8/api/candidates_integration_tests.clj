(ns objective8.api.candidates-integration-tests
  (:require [midje.sweet :refer :all] 
            [peridot.core :as p] 
            [objective8.integration-helpers :as helpers] 
            [objective8.core :as core]
            [objective8.middleware :as m]
            [objective8.writers :as writers]))

(def app (helpers/test-context))

(def OBJECTIVE_ID 1)

(facts "candidates" :integration
       (fact "candidates can be retrieved by objective id"
             (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/candidate-writers"))
             => (helpers/check-json-body {:s "stored-candidates"})
       (provided
         (writers/retrieve-candidates OBJECTIVE_ID) => {:s "stored-candidates"})))

