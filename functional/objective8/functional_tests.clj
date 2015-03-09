(ns objective8.functional-tests 
  (:require [midje.sweet :refer :all]
            [org.httpkit.server :refer [run-server]]
            [clj-webdriver.taxi :as webdriver]
            [objective8.core :as core]
            [dev-helpers.stub-twitter :refer [stub-twitter-auth-config]]))

(def config-without-twitter (assoc core/app-config :authentication stub-twitter-auth-config))

(facts "some tests" :functional
       (against-background 
         [(before :contents (do (core/start-server config-without-twitter)
                             (webdriver/set-driver! {:browser :firefox})))
          (after :contents (do (webdriver/quit)
                            (core/stop-server)))]
       (fact "a test" :functional  
             (do (webdriver/to "localhost:8080")
                 (webdriver/title)) => "Objective[8]"
             (do (webdriver/click "a[href='/objectives']") 
                 (webdriver/title)) => "Objectives | Objective[8]")

       (fact "another test" :functional  
             (do (webdriver/to "localhost:8080")
                 (webdriver/title)) => "Objective[8]"
             (do (webdriver/click "a[href='/objectives']") 
                 (webdriver/title)) => "Objectives | Objective[8]")))  
