(ns d-cent.objectives
  (:require [cemerick.friend :as friend]
            [d-cent.storage :as storage]))

(defn request->objective
  "Returns a map of an objective if all the parts are in the
  request params. Otherwise returns nil"
  [{:keys [params]}]
    (when (= 4 (count params)) (assoc (select-keys params [:title :goals :description :end-date])
                                      :username (get (friend/current-authentication) :username))))


(defn store-objective! [store objective]
  (storage/store! store "objectives" objective))

