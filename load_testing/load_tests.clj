(ns load-tests
  (:require [clj-gatling.core :as g]
            [cheshire.core :as json]
            [org.httpkit.client :as http]))

(declare execute initialise)
(declare signed-out-simulation signed-in-simulation)

(defn execute [simulation users opts] 
  (let [num-users (if (integer? users) users (read-string users))
        simulation-options (merge {:root "tmp"
                                   :timeout-in-ms 5000}
                                  opts)]
    (initialise num-users (:init simulation)) 
    (g/run-simulation (:simulation simulation) 
                      num-users
                      simulation-options)))    

(defn initialise [num-users init-fn]
  (doseq [user-id (range num-users)]
    (init-fn user-id)))

(def base-url "http://192.168.50.50:8080")

(defn- http-get [uri opts user-id context callback]
  (let [check-status (fn [{:keys [status]}] (callback (= 200 status)))]
    (http/get (str base-url uri)
              {:headers {"x-load-test-user-id" (str user-id)
                         "x-load-test-signed-in" (:signed-in opts)}}
              check-status)))

(defn signed-out-simulation [target-uri]
  {:init identity 
   :simulation [{:name "scenario: load pages as signed out user"
                 :requests [{:name "load endpoint" :fn (partial http-get target-uri {:signed-in "false"})}]}]})

(defn signed-in-simulation [target-uri]
  {:init identity
   :simulation [{:name "load pages as signed in user" 
                 :requests [{:name "load endpoint" :fn (partial http-get target-uri {:signed-in "true"})}]}]})

(def simulations
  {:signed-out signed-out-simulation})

