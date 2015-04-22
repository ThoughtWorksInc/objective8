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

(defn put-request [url request]
  @(http/put url request))

(defn get-request
  ([url]
   (get-request url {}))

  ([url options]
   @(http/get url options)))

(defn json-post [url object]
  (post-request url (json-request object)))

(defn default-post-call [url object]
  (let [request (with-credentials (json-request object))
        {:keys [body status]} (post-request url request)]
    (cond
      (= status 201) {:status ::success
                      :result (json/parse-string body true)}
      (= status 400) {:status ::invalid-input}
      :else          {:status ::error})))

(defn default-put-call [url object]
  (let [request (with-credentials (json-request object))
        {:keys [body status]} (put-request url request)]
    (cond
      (= status 200) {:status ::success
                      :result (json/parse-string body true)}
      (= status 404) {:status ::not-found}
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
       (= status 403) {:status ::forbidden}
       :else          {:status ::error}))))

;;USERS

(defn create-user [user]
  (default-post-call (str utils/host-url "/api/v1/users") user))

(defn find-user-by-twitter-id [twitter-id]
  (default-get-call
    (str utils/host-url "/api/v1/users?twitter=" twitter-id)
    {:headers (get-api-credentials)}))

(defn find-user-by-username [username]
  (default-get-call
    (str utils/host-url "/api/v1/users?username=" username)
    {:headers (get-api-credentials)}))

(defn get-user [user-id]
  (default-get-call
    (str utils/host-url "/api/v1/users/" user-id)
    {:headers (get-api-credentials)}))

;; OBJECTIVES

(defn create-objective [objective]
  (default-post-call
    (str utils/host-url "/api/v1/objectives")
    (update-in objective [:end-date] str)))

(defn parse-objective [raw-objective]
  (update-in raw-objective [:end-date] utils/time-string->date-time))

(defn get-objective
  ([id] (get-objective id {}))

  ([id query]
   (let [api-result (default-get-call (str utils/host-url "/api/v1/objectives/" id)
                      (assoc {:headers (get-api-credentials)}
                             :query-params query))]
     (if (= ::success (:status api-result))
       (update-in api-result [:result] parse-objective)
       api-result))))

(defn query->objectives-query-params [query]
  (let [user-id (:signed-in-id query)]
    (cond-> {:headers (get-api-credentials)}
      user-id (assoc :query-params {:user-id user-id}))))

(defn get-objectives
  ([]
   (get-objectives {}))

  ([query]
   (let [api-result (default-get-call
                      (str utils/host-url "/api/v1/objectives")
                      (query->objectives-query-params query))]
     (if (= ::success (:status api-result))
       (update-in api-result [:result] #(map parse-objective %))
       api-result))))

;; COMMENTS
(defn post-comment [comment-data]
  (default-post-call (utils/path-for :api/post-comment) comment-data))

(defn get-comments [entity-uri]
  (default-get-call (utils/path-for :api/get-comments) {:query-params {:uri entity-uri}}))

;; QUESTIONS

(defn create-question [question]
  (default-post-call (str utils/host-url "/api/v1/objectives/" (:objective-id question) "/questions") question))

(defn get-question [objective-id question-id]
  (default-get-call
    (str utils/host-url "/api/v1/objectives/" objective-id "/questions/" question-id)))

(defn retrieve-questions
  ([objective-id]
   (retrieve-questions objective-id {}))

  ([objective-id options]
   (default-get-call
     (str utils/host-url "/api/v1/objectives/" objective-id "/questions")
     {:query-params options})))

(defn post-mark [mark-data]
  (default-post-call (utils/path-for :api/post-mark) mark-data))

;; ANSWERS

(defn create-answer [answer]
  (default-post-call
    (str utils/host-url
         "/api/v1/objectives/" (:objective-id answer)
         "/questions/" (:question-id answer) "/answers")
    answer))

(defn retrieve-answers [question-uri]
  (default-get-call
    (str utils/host-url "/api/v1" question-uri "/answers")))

;; WRITERS

(defn create-invitation [invitation]
  (default-post-call
    (str utils/host-url "/api/v1/objectives/" (:objective-id invitation) "/writer-invitations") invitation))

(defn retrieve-invitation-by-uuid [uuid]
  (default-get-call
    (str utils/host-url "/api/v1/invitations?uuid=" uuid)))

(defn decline-invitation [{:keys [objective-id invitation-id] :as invitation}]
  (default-put-call
    (str utils/host-url "/api/v1/objectives/" objective-id "/writer-invitations/" invitation-id)
    invitation))

(defn retrieve-writers [objective-id]
  (default-get-call
    (str utils/host-url "/api/v1/objectives/" objective-id "/writers")))

(defn post-writer [writer]
  (default-post-call (str utils/host-url
                          "/api/v1/objectives/" (:objective-id writer)
                          "/writers") writer))

(defn post-profile [profile-data]
  (default-put-call (utils/path-for :api/put-writer-profile) profile-data))

;; DRAFTS

(defn get-draft [objective-id draft-id]
  (default-get-call (utils/path-for :api/get-draft :id objective-id :d-id draft-id)))

(defn post-draft [{objective-id :objective-id :as draft}]
  (default-post-call (utils/path-for :api/post-draft :id objective-id) draft))

(defn get-all-drafts [objective-id]
  (default-get-call (utils/path-for :api/get-drafts-for-objective :id objective-id)))

;; VOTES

(defn create-up-down-vote [vote]
  (default-post-call (utils/path-for :api/post-up-down-vote) vote))

;; STARS

(defn post-star [star-data]
  (default-post-call (utils/path-for :api/post-star) star-data))
