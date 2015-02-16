(ns objective8.bearer-tokens
 (:require [objective8.storage.storage :as storage]))

(defn get-token [name]
  (let [{result :result} (storage/pg-retrieve {:entity :bearer-token :bearer-name name})]
    (dissoc (first result) :entity)))
