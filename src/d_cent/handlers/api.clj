(ns d-cent.handlers.api
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [d-cent.storage.storage :as storage]
            [d-cent.objectives :as objectives]
            [d-cent.comments :as comments]
            [d-cent.user :as user]
            [d-cent.utils :as utils]))

;; USER
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

;; OBJECTIVE
(defn post-objective [{:keys [params] :as request}]
  (let [objective (-> params
                      (select-keys [:title :goals :description :end-date :created-by]))
        stored-objective (objectives/store-objective! objective)
        resource-location (str utils/host-url "/api/v1/objectives/" (:_id stored-objective))]
    {:status 201
     :headers {"Content-Type" "application/json"
               "Location" resource-location}
     :body stored-objective}))

(defn get-objective [{:keys [route-params] :as request}]
  (try
    (let [id (-> (:id route-params)
                 Integer/parseInt)]
      (if-let [objective (objectives/retrieve-objective id)]
        (-> objective
            (update-in [:end-date] utils/date-time->iso-date-string)
            response/response
            (response/content-type "application/json"))
        (response/not-found "")))
    (catch NumberFormatException e
      {:status 400
       :header {}
       :body "Objective id must be an integer"})))

;; COMMENT
(defn post-comment [{:keys [params] :as request}]
  (let [store (storage/request->store request)
        comment (-> params
                  (select-keys [:comment :objective-id :user-id]))
        stored-comment (comments/store-comment! store comment)
        resource-location (str utils/host-url "/api/v1/comments/" (:_id stored-comment))]
    {:status 201
     :headers {"Content-Type" "application/json"
               "Location" resource-location}
     :body stored-comment}))
