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

(defn api-objective-post [request]
  (let [store (storage/request->store request)
        title (get-in request [:params :title])
        goals (get-in request [:params :goals])
        description (get-in request [:params :description])
        end-date (get-in request [:params :end-date])
        created-by (get-in request [:params :created-by])]
    (objectives/store-objective! store {:title title :goals goals :description description
                                       :end-date end-date :created-by created-by})

    {:status 201 :headers {"Location" "value"}}))

