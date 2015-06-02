(ns load-tests
  (:require [clj-gatling.core :as g]
            [org.httpkit.client :as http]))

(declare ping-simulation execute)

(defn -main [target-uri users requests]
  (execute target-uri users requests {})) 

(defn execute [target-uri users requests opts] 
  (let [num-users (if (integer? users) users (read-string users))
        num-requests (if (integer? requests) requests (read-string requests))
        simulation-options (merge {:root "tmp" :requests num-requests :timeout-in-ms 5000} opts)]
    (g/run-simulation (ping-simulation target-uri) 
                      num-users
                      simulation-options)))    

(def base-url "http://192.168.50.50:8080")

(defn- http-get [url user-id context callback]
  (let [check-status (fn [{:keys [status]}] (callback (= 200 status)))]
    (http/get (str base-url url) {} check-status)))

(defn ping [target-uri]
  (partial http-get target-uri))

(defn ping-simulation [target-uri]
  [{:name "Ping scenario"
    :requests [{:name "Ping endpoint" :fn (ping target-uri)}]}])

(def simulations
  {:ping ping-simulation})

