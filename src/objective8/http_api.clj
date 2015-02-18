(ns objective8.http-api
  (:require [org.httpkit.client :as http]
            [ring.util.response :as response]
            [objective8.utils :as utils]
            [cheshire.core :as json]))

(def api-failure nil)

(defn get-api-credentials []
  {"api-bearer-name" "bearer"
   "api-bearer-token" "token"})

(defn json-request [object]
  {:headers {"Content-Type" "application/json"}
   :body (json/generate-string object)})

(defn with-credentials [request]
  (update-in request [:headers] merge (get-api-credentials)))

(defn post-request [url request]
  @(http/post url request))

(defn json-post [url object]
  (post-request url (json-request object)))

;;USERS

(defn create-user [user]
  (let [request (with-credentials (json-request user))
        {:keys [body status]} (post-request (str utils/host-url "/api/v1/users") request)]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))

(defn find-user-by-twitter-id [twitter-id]
  (let [{:keys [body status]} @(http/get (str utils/host-url "/api/v1/users?twitter=" twitter-id))]
    (cond (= status 200) (-> body (json/parse-string true))
      :else          api-failure)))


;; OBJECTIVES

(defn create-objective [objective]
  (let [request (with-credentials (json-request (update-in  objective [:end-date] str)))
        {:keys [body status]} (post-request (str utils/host-url "/api/v1/objectives") request)]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))

(defn get-objective [id]
  (let [{:keys [body status]} @(http/get (str utils/host-url "/api/v1/objectives/" id))]
    (cond
      (= status 200) (-> body
                         (json/parse-string true)
                         (update-in [:end-date] utils/time-string->date-time))
      (= status 404) (response/not-found "")
      :else          api-failure)))


;; COMMENTS

(defn create-comment [comment]
  (let [request (with-credentials (json-request comment))
        {:keys [body status]} (post-request (str utils/host-url "/api/v1/comments") request)]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))

(defn retrieve-comments [objective-id]
  (let [{:keys [body status]} @(http/get (str utils/host-url "/api/v1/objectives/" objective-id "/comments"))]
    (cond
      (= status 200) (-> body
                         (json/parse-string true))
      :else          api-failure)))

;; QUESTIONS

(defn create-question [question]
  (let [request (with-credentials (json-request question))
        {:keys [body status]} (post-request (str utils/host-url "/api/v1/objectives/" (:objective-id question) "/questions") request)]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))

(defn get-question [objective-id question-id]
  (let [{:keys [body status]} @(http/get (str utils/host-url "/api/v1/objectives/" objective-id "/questions/" question-id))]
    (cond
      (= status 200) {:status ::success
                      :result (-> body
                                  (json/parse-string true))}
      (= status 404) {:status ::not-found}
      :else          {:status ::error})))

;; ANSWERS

(defn create-answer [answer]
  (let [{:keys [body status]} (json-post (str utils/host-url "/api/v1/objectives/" (:objective-id answer) 
                                              "/questions/" (:question-id answer) "/answers") answer)]
    (cond
      (= status 201) {:status ::success
                      :result (json/parse-string body true)}
      :else          {:status ::error})))
