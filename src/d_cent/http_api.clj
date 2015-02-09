(ns d-cent.http-api
  (:require [org.httpkit.client :as http]
            [d-cent.utils :as utils]
            [cheshire.core :as json]))

(def api-failure nil)

(defn json-post [url object]
  @(http/post url {:headers {"Content-Type" "application/json"}
                   :body (json/generate-string object)}))

;;USERS

(defn create-user [user]
  (let [{:keys [body status]} (json-post (str utils/host-url "/api/v1/users") user)]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))

(defn find-user-by-twitter-id [twitter-id]
  (let [{:keys [body status]} @(http/get (str utils/host-url "/api/v1/users?twitter=" twitter-id))]
    (cond (= status 200) (-> body (json/parse-string true))
      :else          api-failure)))


;; OBJECTIVES

(defn create-objective [objective]
  (let [{:keys [body status]} (json-post (str utils/host-url "/api/v1/objectives") (update-in objective [:end-date] str))]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))

(defn get-objective [id]
  (let [{:keys [body status]} @(http/get (str utils/host-url "/api/v1/objectives/" id))]
    (cond
      (= status 200) (-> body
                         (json/parse-string true)
                         (update-in [:end-date] utils/time-string->date-time))
      :else          api-failure)))


;; COMMENTS

(defn create-comment [comment]
  (let [{:keys [body status]} (json-post (str utils/host-url "/api/v1/comments") comment)]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))
