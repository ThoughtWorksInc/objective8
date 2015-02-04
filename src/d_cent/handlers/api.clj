(ns d-cent.handlers.api
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [d-cent.storage :as storage]
            [d-cent.objectives :as objectives]
            [d-cent.user :as user]
            [d-cent.utils :as utils]))

(defn post-user-profile [request]
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

(defn get-user-profile [request]
  (let [store (storage/request->store request)
        twitter-id (get-in request [:params :twitter])]
    (if-let [user-profile (user/retrieve-user-record store twitter-id)]
      (response/content-type (response/response user-profile) "application/json")
      (response/not-found ""))))

(defn post-objective [{:keys [params] :as request}]
  (let [store (storage/request->store request)
        objective (-> params
                      (select-keys [:title :goals :description :end-date :created-by])
                      (update-in [:end-date] utils/time-string->date-time))
        stored-objective (-> (objectives/store-objective! store objective)
                             (update-in [:end-date] utils/date-time->iso-date-string))
        resource-location (str utils/host-url "/api/v1/objectives/" (:_id stored-objective))]
    {:status 201
     :headers {"Content-Type" "application/json"
               "Location" resource-location}
     :body stored-objective}))

(defn get-objective [{:keys [route-params] :as request}]
  (let [store (storage/request->store request)
        id (:id route-params)]
    (if-let [objective (objectives/retrieve-objective store id)]
      (-> objective
          (update-in [:end-date] utils/date-time->iso-date-string)
          response/response
          (response/content-type "application/json"))
      (response/not-found ""))))
