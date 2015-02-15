(ns user
  (:require [clojure.tools.namespace.repl :as tnr]
            [org.httpkit.server :as server]
            [objective8.core :as core]
            [objective8.config :as config]
            [objective8.storage.database :as db]
            [objective8.utils :as utils]
            [dev-helpers.stub-twitter :refer [stub-twitter-auth-config]]
            [dev-helpers.launch :refer [stop make-launcher-map]]))

; Don't try to load ./test and ./integration
(tnr/set-refresh-dirs "./src" "./dm")

(def configs 
  {:default core/app-config
   :stub-twitter (assoc core/app-config :authentication stub-twitter-auth-config)})

(def launchers (make-launcher-map configs))

(defn reset [config-key]
  (let [post-reset-hook (get launchers config-key (:default launchers))]
    (stop)
    (tnr/refresh :after post-reset-hook)
    (prn "Reset")
    config-key))
