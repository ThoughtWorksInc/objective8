(ns main
  (:use [org.httpkit.server :only [run-server]]
        [bidi.ring :only [make-handler]])
  (:require [clojure.tools.logging :as log]
            [responses :refer :all]))

(defonce server (atom nil))

(defn test-handler [request] 
  (simple-response (str "test handler: " (:id (:params request)))))

(def handlers {:index (constantly (rendered-response "index.html"))
               :test test-handler})

(def app
  (make-handler ["/" {"" :index 
                      [:id "/test"] :test}] handlers))

(defn start-server []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
    (log/info (str "Starting d-cent on port " port))
    (reset! server (run-server app {:port port}))))

(defn -main []
  (start-server))

(defn stop-server []
  (when-not (nil? @server)
    (@server)
    (reset! server nil)))

(defn restart-server []
  (stop-server)
  (start-server))
