(ns dev-helpers.launch
  (:require [org.httpkit.server :as server]
            [clojure.tools.logging :as log]
            [objective8.core :as core]
            [objective8.config :as config]
            [objective8.back-end.storage.database :as db]))

;; Launching / relaunching / loading
(defonce the-system nil)

(defn- start-api-server [system]
  (let [conf (:config system)
        api-port (:api-port system)
        server (server/run-server (core/api-handler conf) {:port api-port})]
    (prn "Starting api server on port: " api-port)
    (assoc system :api-server server)))

(defn- stop-api-server [system]
  (when-let [srv (:api-server system)]
    (srv))
  (dissoc system :api-server))

(defn- start-front-end-server [system]
  (let [conf (:config system)
        front-end-port (:front-end-port system)
        server (server/run-server (core/front-end-handler conf) {:port front-end-port})]
    (prn "Starting front-end server on port: " front-end-port)
    (assoc system :front-end-server server)))

(defn- stop-front-end-server [system]
  (when-let [srv (:front-end-server system)]
    (srv))
  (dissoc system :front-end-server))

(defn- init 
  ([system]
   (init system core/app-config))

  ([system conf]
   (let [db-connection (db/connect!)]
     (core/initialise-api)
     (assoc system
            :config conf
            :front-end-port (:front-end-port config/environment)
            :api-port (:api-port config/environment)
            :db-connection db-connection))))

(defn- make-launcher [config-name launcher-config]
  (fn []
    (alter-var-root #'the-system #(-> %
                                      (init launcher-config)
                                      start-front-end-server
                                      start-api-server))
    (log/info (str "Objective8 started\nfront-end on port: " (:front-end-port the-system)
                   "\napi on port:" (:api-port the-system)
                   " in configuration " config-name))))

(defn stop []
  (alter-var-root #'the-system #(-> %
                                    stop-api-server
                                    stop-front-end-server))
  (log/info "Objective8 server stopped."))

(defn make-launcher-map [configs]
  (doall 
   (apply merge
          (for [[config-kwd config] configs]
            (let [config-name (name config-kwd)
                  launcher-name (str "start-" config-name)]

              (intern *ns* 
                      (symbol launcher-name) 
                      (make-launcher config-name config))

              {config-kwd (symbol (str "user/" launcher-name))})))))
