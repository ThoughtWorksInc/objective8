(ns objective8.back-end.domain.bearer-tokens
 (:require [objective8.back-end.storage.storage :as storage]))

(defn get-token [name]
  (-> (storage/pg-retrieve {:entity :bearer-token :bearer-name name}) 
      :result
      first))

(defn update-token! [token-details]
  (storage/pg-update-bearer-token! (assoc token-details :entity :bearer-token)))

(defn store-token! [token-details]
  (storage/pg-store! (assoc token-details :entity :bearer-token)))

(defn token-provider [bearer-name]
  (:bearer-token (get-token bearer-name)))

(defn stub-token-provider [bearer-name]
  (when (= bearer-name "bearer") "token"))
