(ns d-cent.objectives
  (:require [cemerick.friend :as friend]
            [d-cent.storage :as storage]))

(defn request->objective
  "Returns a map of an objective if all the parts are in the
  request params. Otherwise returns nil"
  [{:keys [params]}]
    ;TODO Should we use friend here?
    (when (= 4 (count params)) (assoc (select-keys params [:title :goals :description :end-date])
                                      :created-by (get (friend/current-authentication) :username))))


(defn store-objective! [store objective]
  (storage/store! store "objectives" objective))

(defn find-by-id [store id]
  (storage/find-by store "objectives" #(= id (:_id %))))
