(ns objective8.front-end-helpers
  (:require [cemerick.friend :as friend]))

(defn request->question
  "Returns a map of a question with a user if all parts are in the request. Otherwise returns nil"
  [{{id :id} :route-params
    :keys [params]} user-id]
  (assoc (select-keys params [:question])
          :created-by-id user-id
          :objective-id (Integer/parseInt id)))

(defn request->comment
  [{:keys [params]} user-id]
  (let [objective-id (Integer/parseInt (params :objective-id))]
    (assoc (select-keys params [:comment])
            :objective-id objective-id
            :created-by-id user-id)))
