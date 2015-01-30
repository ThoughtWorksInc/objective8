(ns d-cent.api
  (:require [org.httpkit.client :as http]
            [d-cent.utils :as utils]
            [cheshire.core :as json]))

(def api-failure nil)

(defn post-user-profile [user-profile]
  (let [{:keys [body status]} @(http/post (str utils/host-url "/api/v1/users")
                                {:headers {"Content-Type" "application/json"}
                                 :body (json/generate-string user-profile)})]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))
