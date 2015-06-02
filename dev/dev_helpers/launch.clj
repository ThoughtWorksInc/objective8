(ns dev-helpers.launch
  (:require [org.httpkit.server :as server]
            [clojure.tools.logging :as log]
            [objective8.core :as core]
            [objective8.config :as config]
            [objective8.back-end.storage.database :as db]))

;; Launching / relaunching / loading
(defonce the-system nil)

(defn- start-server []
  (let [conf (:config the-system)
        port (:port the-system)
        server (server/run-server (core/app conf) {:port port})]
    (assoc the-system :server server)))

(defn- stop-server [the-system]
  (when-let [srv (:server the-system)]
    (srv)))

(defn- init 

  ([]
   (init core/app-config))

  ([conf]
   (let [port (:front-end-port config/environment)
         db-connection (db/connect!)]
     (core/initialise-api)
     (alter-var-root #'the-system
                     (constantly {:config conf
                                  :port port
                                  :db-connection db-connection})))))

(defn- make-launcher [config-name launcher-config]
  (fn []
    (init launcher-config)
    (alter-var-root #'the-system (constantly (start-server)))
    (log/info (str "Objective8 server started on port: " (:port the-system)
                   " in configuration " config-name))))

(defn stop []
  (alter-var-root #'the-system stop-server)
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
