(ns objective8.writers
  (:require 
    [objective8.storage.storage :as storage]))  

(defn store-invited-writer! [writer]
  (storage/pg-store! (assoc writer :entity :invitation)))
