(ns d-cent.handlers.api
  (:require [clojure.tools.logging :as log]
            [d-cent.storage :as storage]
            [d-cent.user :as user]))

(defn api-user-profile-post [request]
  (log/info (:params request))
  (let [store (storage/request->store request)
        user-id (get-in request [:params :user-id])
        email-address (get-in request [:params :email-address])]
    (user/store-user-profile! store {:user-id user-id :email-address email-address})
    {:status 200}))

(defn api-objective-post []
  )

