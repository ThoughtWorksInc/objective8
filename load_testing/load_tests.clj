(ns load-tests
  (:require [clj-gatling.core :as g]
            [org.httpkit.client :as http]))

(declare simulations)

(defn -main [simulation users requests]
  (let [simulation (or ((keyword simulation) simulations)
                       (throw (Exception. (str "No such simulation " simulation))))]
    (g/run-simulation simulation
                            (read-string users)
                            {:root "tmp" :requests (read-string requests)})))

(def base-url "http://192.168.50.50:8080")

(defn- http-get [url user-id context callback]
  (let [check-status (fn [{:keys [status]}] (callback (= 200 status)))]
    (http/get (str base-url url) {} check-status)))

(def ping
  (partial http-get "/objectives/70910"))

(def ping-simulation
  [{:name "Ping scenario"
    :requests [{:name "Ping endpoint" :fn ping}]}])

(def simulations
  {:ping ping-simulation})

