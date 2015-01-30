(ns d-cent.http-api
  (:require [org.httpkit.client :as http]
            [d-cent.utils :as utils]
            [cheshire.core :as json]))

(def api-failure nil)

(defn json-post [url object]
  @(http/post url {:headers {"Content-Type" "application/json"}
                   :body (json/generate-string object)}))

(defn create-user-profile [user-profile]
  (let [{:keys [body status]} (json-post "/api/v1/users" user-profile)]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))

(defn create-objective [objective]
  (let [{:keys [body status]} (json-post "/api/v1/objectives" objective)]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))
