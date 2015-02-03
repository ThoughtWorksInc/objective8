(ns d-cent.http-api
  (:require [org.httpkit.client :as http]
            [d-cent.utils :as utils]
            [cheshire.core :as json]))

(def api-failure nil)

(defn json-post [url object]
  @(http/post url {:headers {"Content-Type" "application/json"}
                   :body (json/generate-string object)}))

(defn create-user-profile [user-profile]
  (let [{:keys [body status]} (json-post (str utils/host-url "/api/v1/users") user-profile)]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))

(defn find-user-profile-by-twitter-id [twitter-id]
  (let [api-response @(http/get (str utils/host-url "/api/v1/users?twitter=" twitter-id))]
    (json/parse-string (api-response :body))))

(defn create-objective [objective]
  (let [{:keys [body status]} (json-post (str utils/host-url "/api/v1/objectives") (assoc objective :end-date (str (objective :end-date))))]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))

(defn get-objective [guid]
  (let [{:keys [body status]} @(http/get (str utils/host-url "/api/v1/objectives/" guid))]
    (cond 
      (= status 200) (-> body 
                         (json/parse-string true)
                         (update-in [:end-date] utils/time-string->time-stamp))
      :else          api-failure)))
