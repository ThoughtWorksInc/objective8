(ns d-cent.handlers.api
  (:require [clojure.tools.logging :as log]
            [d-cent.storage :as storage]
            [d-cent.objectives :as objectives]
            [d-cent.user :as user]
            [d-cent.utils :as utils]))

(defn api-user-profile-post [request]
  (let [store (storage/request->store request)
        user-id (get-in request [:params :user-id])
        email-address (get-in request [:params :email-address])
        user-profile (user/store-user-profile! store
                                               {:user-id user-id
                                                :email-address email-address})
        resource-location (str utils/host-url "/api/v1/users/" (:_id user-profile))]
    {:status 201
     :headers {"Content-Type" "application/json"
               "Location" resource-location}
     :body user-profile}))

(defn api-objective-post [{:keys [params] :as request}]
  (let [store (storage/request->store request)
        objective (select-keys params [:title :goals :description :end-date :created-by])
        stored-objective (objectives/store-objective! store objective)
        resource-location (str utils/host-url "/api/v1/objectives/" (:_id stored-objective))]
    {:status 201
     :headers {"Content-Type" "application/json"
               "Location" resource-location}
     :body stored-objective}))
