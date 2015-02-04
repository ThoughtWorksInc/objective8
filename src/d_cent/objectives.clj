(ns d-cent.objectives
  (:require [cemerick.friend :as friend]
            [d-cent.utils :as utils]
            [d-cent.storage :as storage]))

(defn request->objective
  "Returns a map of an objective if all the parts are in the
  request params. Otherwise returns nil"
  [{:keys [params]}]
    ;TODO Should we use friend here?
    (let [iso-time (utils/string->date-time (:end-date params))]
      (when (= 4 (count params)) (assoc (select-keys params [:title :goals :description])
                                  :end-date iso-time
                                  :created-by (get (friend/current-authentication) :username)))))

(defn format-objective [objective]
  (update-in objective [:end-date] str))

(defn store-objective! [store objective]
  (format-objective (storage/store! store "objectives" (update-in objective [:end-date] utils/time-string->date-time))))

(defn find-by-id [store id]
  (if-let [stored-objective (storage/find-by store "objectives" #(= id (:_id %)))]
    (format-objective stored-objective)))
