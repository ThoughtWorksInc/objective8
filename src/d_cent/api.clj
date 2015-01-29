(ns d-cent.api
  (:require [org.httpkit.client :as http]
            [d-cent.utils :as utils]
            [cheshire.core :as json]))

(defn post-user-profile [user-profile]
  @(http/post (str utils/host-url "/api/v1/users")
                                 {:headers {"Content-Type" "application/json"}
                                  :body (json/generate-string user-profile)}))
