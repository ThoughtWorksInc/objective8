(ns d-cent.objectives
  (:require [cemerick.friend :as friend]
            [d-cent.utils :as utils]
            [d-cent.storage.storage :as storage]))

(defn request->objective
  "Returns a map of an objective if all the parts are in the
  request params. Otherwise returns nil"
  [{:keys [params]}]
    ;TODO Should we use friend here?
    (let [iso-time (utils/string->date-time (:end-date params))]
      (assoc (select-keys params [:title :goals :description])
                                  :end-date iso-time
                                  :created-by (get (friend/current-authentication) :username))))

(defn store-objective! [store objective]
  (storage/store! store "objectives" objective))

(defn retrieve-objective [store guid]
  (storage/retrieve store "objectives" guid))
