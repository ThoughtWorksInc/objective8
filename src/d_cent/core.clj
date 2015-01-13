(ns d-cent.core
  (:use [org.httpkit.server :only [run-server]])
  (:require [clojure.tools.logging :as log]
            [bidi.ring :refer [make-handler ->Resources]]
            [taoensso.tower.ring :refer [wrap-tower]]
            [d-cent.responses :refer :all]
            [d-cent.translation :refer [translation-config]]))

(defonce server (atom nil))

(defn index [{:keys [t']}]
  (simple-response (t' :index/welcome)))

(def handlers {:index (wrap-tower index translation-config)})

(def app
  (make-handler ["/" {""        :index 
                      "static/" (->Resources {:prefix "public/"})}]
                ;; We need this filtering to make the static handler work
                (some-fn handlers #(when (fn? %) %))))

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
