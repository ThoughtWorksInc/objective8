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
      (= status 400) {:status ::invalid-input}
      :else          {:status ::error})))

(defn default-get-call
  ([url]
   (default-get-call url {}))

  ([url options]
  (let [{:keys [body status]} (get-request url options)]
    (cond
      (= status 200) {:status ::success
                      :result (-> body
                                  (json/parse-string true))}
      (= status 404) {:status ::not-found}
      (= status 400) {:status ::invalid-input}
      :else          {:status ::error}))))

;;USERS

(defn create-user [user]
  (default-create-call (str utils/host-url "/api/v1/users") user))

(defn find-user-by-twitter-id [twitter-id]
  (default-get-call
    (str utils/host-url "/api/v1/users?twitter=" twitter-id)
    {:headers (get-api-credentials)}))


;; OBJECTIVES

(defn create-objective [objective]
  (default-create-call
    (str utils/host-url "/api/v1/objectives")
    (update-in objective [:end-date] str)))

(defn parse-objective [raw-objective]
  (update-in raw-objective [:end-date] utils/time-string->date-time))

(defn get-objective [id]
   (let [api-result (default-get-call (str utils/host-url "/api/v1/objectives/" id))]
     (if (= ::success (:status api-result))
       (update-in api-result [:result] parse-objective)
       api-result)))

(defn get-all-objectives []
  (let [api-result (default-get-call (str utils/host-url "/api/v1/objectives"))]
    (if (= ::succes (:status api-result))
      (update-in api-result [:result] (partial map parse-objective))
      api-result)))

;; COMMENTS

(defn create-comment [comment]
  (default-create-call (str utils/host-url "/api/v1/comments") comment))

(defn retrieve-comments [objective-id]
  (default-get-call (str utils/host-url "/api/v1/objectives/" objective-id "/comments")))

;; QUESTIONS

(defn create-question [question]
  (default-create-call (str utils/host-url "/api/v1/objectives/" (:objective-id question) "/questions") question))

(defn get-question [objective-id question-id]
  (default-get-call
    (str utils/host-url "/api/v1/objectives/" objective-id "/questions/" question-id)))

(defn retrieve-questions [objective-id]
  (default-get-call
    (str utils/host-url "/api/v1/objectives/" objective-id "/questions")))

;; ANSWERS

(defn create-answer [answer]
  (default-create-call
    (str utils/host-url
         "/api/v1/objectives/" (:objective-id answer)
         "/questions/" (:question-id answer) "/answers")
    answer))

(defn retrieve-answers [objective-id question-id]
  (default-get-call
    (str utils/host-url "/api/v1/objectives/" objective-id "/questions/" question-id "/answers")))
