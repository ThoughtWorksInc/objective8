(ns objective8.objectives
  (:require [cemerick.friend :as friend]
            [objective8.utils :as utils]
            [objective8.storage.storage :as storage]))

(defn request->objective
  "Returns a map of an objective if all the parts are in the
  request params. Otherwise returns nil"
  [{:keys [params]}]
    ;TODO Should we use friend here?
    (let [iso-time (utils/string->date-time (:end-date params))]
      (assoc (select-keys params [:title :goals :description])
                                  :end-date iso-time
                                  :created-by-id (get (friend/current-authentication) :username))))

(defn store-objective! [objective]
  (storage/pg-store! (assoc objective :entity :objective)))

(defn retrieve-objective [objective-id]
  (let [{result :result} (storage/pg-retrieve {:entity :objective :_id objective-id})]
    (dissoc (first result) :entity)))
