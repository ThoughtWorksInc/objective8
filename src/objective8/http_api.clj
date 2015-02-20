(ns objective8.http-api
  (:require [org.httpkit.client :as http]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.config :as config]))

(def api-failure nil)

(def consumer {:consumer-name (config/get-var "API_BEARER_NAME")
               :consumer-token (config/get-var "API_BEARER_TOKEN")})

(defn get-api-credentials []
  {"api-bearer-name" (consumer :consumer-name)
   "api-bearer-token" (consumer :consumer-token)})

(defn json-request [object]
  {:headers {"Content-Type" "application/json"}
   :body (json/generate-string object)})

(defn with-credentials [request]
  (update-in request [:headers] merge (get-api-credentials)))

(defn post-request [url request]
  @(http/post url request))

(defn get-request 
  ([url]
   (get-request url {}))

  ([url options] 
   @(http/get url options)))

(defn json-post [url object]
  (post-request url (json-request object)))

(defn default-create-call [url object]
  (let [request (with-credentials (json-request object))
        {:keys [body status]} (post-request url request)]
    (cond
      (= status 201) {:status ::success
                      :result (json/parse-string body true)}
      :else          {:status ::error})))

;;USERS

(defn create-user [user]
  (let [request (with-credentials (json-request user))
        {:keys [body status]} (post-request (str utils/host-url "/api/v1/users") request)]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))

(defn find-user-by-twitter-id [twitter-id]
  (let [request-options {:headers (get-api-credentials)}
        {:keys [body status]} (get-request (str utils/host-url "/api/v1/users?twitter=" twitter-id) request-options)]
    (cond (= status 200) (-> body (json/parse-string true))
          :else          api-failure)))


;; OBJECTIVES

(defn create-objective [objective]
  (let [request (with-credentials (json-request (update-in  objective [:end-date] str)))
        {:keys [body status]} (post-request (str utils/host-url "/api/v1/objectives") request)]
    (cond (= status 201)   (json/parse-string body true)
          :else            api-failure)))

(defn parse-objective [raw-objective]
  (update-in raw-objective [:end-date] utils/time-string->date-time))

(defn get-objective [id]
  (let [{:keys [body status]} @(http/get (str utils/host-url "/api/v1/objectives/" id))]
    (cond
      (= status 200) (-> body
                         (json/parse-string true)
                         parse-objective)
      (= status 404) (response/not-found "")
      :else          api-failure)))

(defn get-all-objectives []
  (let [{:keys [body status]} (get-request (str utils/host-url "/api/v1/objectives"))]
    (cond (= status 200) {:status ::success 
                          :result (map parse-objective (json/parse-string body true))}
          :else          {:status ::error})))
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
  (default-create-call 
    (str utils/host-url
         "/api/v1/objectives/" (:objective-id answer)
         "/questions/" (:question-id answer) "/answers")
    answer))

(defn retrieve-answers [objective-id question-id]
  (let [{:keys [body status]} @(http/get (str utils/host-url "/api/v1/objectives/" 
                                              objective-id "/questions/" question-id "/answers"))]
    (cond
      (= status 200) (-> body
                         (json/parse-string true))
      :else          api-failure)))



