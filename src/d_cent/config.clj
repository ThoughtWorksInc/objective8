(ns d-cent.config
 (:require [clojure.tools.logging :as log]))

(def ^:dynamic enable-csrf true)

(defn- env-lookup [var-name]
  (get (System/getenv) var-name))

(defn get-var 
  "Attempts to read an environment variable. If no variable is
  found will log a warning message and use the default. If no
  default is provided will use nil"
  ([var-name]
   (get-var var-name nil))
  ([var-name default] 
  (if-let [variable (get (System/getenv) var-name)]
    variable
    (do (log/warn (str "Failed to look up environment variable \"" var-name "\""))
        default))))
