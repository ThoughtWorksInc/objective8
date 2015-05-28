(ns main
  (:require [objective8.core :as core]
            [ragtime.main :as rm])
  (:gen-class)) 
   
(defn -main []
  (prn rm/help-text)
 (core/start-server))
