(ns objective8.front-end.api.http
  (:require [org.httpkit.client :as http]
            [ring.util.codec :as codec]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.config :as config]))

(def api-failure nil)

(def consumer (:api-credentials config/environment))

(defn get-api-credentials []
  {"api-bearer-name" (consumer :bearer-name)
   "api-bearer-token" (consumer :bearer-token)})

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
  (default-post-call (str utils/api-url "/api/v1/users") user))

(defn find-user-by-auth-provider-user-id [auth-provider-user-id]
  (default-get-call
    (str utils/api-url "/api/v1/users?auth_provider_user_id=" auth-provider-user-id)
    {:headers (get-api-credentials)}))

(defn find-user-by-username [username]
  (default-get-call
    (str utils/api-url "/api/v1/users?username=" username)
    {:headers (get-api-credentials)}))

(defn get-user [user-id]
  (default-get-call
    (str utils/api-url "/api/v1/users/" user-id)
    {:headers (get-api-credentials)}))

;; OBJECTIVES

(defn create-objective [objective]
  (default-post-call
    (str utils/api-url "/api/v1/objectives") objective))

(defn get-objective
  ([id] (get-objective id {}))

  ([id query]
   (default-get-call (str utils/api-url "/api/v1/objectives/" id)
     (assoc {:headers (get-api-credentials)}
            :query-params query))))

(defn query->objectives-query-params [query]
  (let [user-id (:signed-in-id query)]
    (cond-> {:headers (get-api-credentials)}
      user-id (assoc :query-params {:user-id user-id}))))

(defn get-objectives
  ([]
   (get-objectives {}))

  ([query]
   (default-get-call
     (str utils/api-url "/api/v1/objectives")
     (query->objectives-query-params query))))

(defn get-objectives-for-writer [user-id]
  (default-get-call (utils/api-path-for :api/get-objectives-for-writer :id user-id)))

;; COMMENTS
(defn post-comment [comment-data]
  (default-post-call (utils/api-path-for :api/post-comment) comment-data))

(defn get-comments
  ([entity-uri]
   (get-comments entity-uri {}))

  ([entity-uri options]
   (default-get-call (utils/api-path-for :api/get-comments) {:query-params (merge {:uri entity-uri} options)})))

;; QUESTIONS

(defn create-question [question]
  (default-post-call (str utils/api-url "/api/v1/objectives/" (:objective-id question) "/questions") question))

(defn get-question [objective-id question-id]
  (default-get-call
    (str utils/api-url "/api/v1/objectives/" objective-id "/questions/" question-id)))

(defn retrieve-questions
  ([objective-id]
   (retrieve-questions objective-id {}))

  ([objective-id options]
   (default-get-call
     (str utils/api-url "/api/v1/objectives/" objective-id "/questions")
     {:query-params options})))

(defn post-mark [mark-data]
  (default-post-call (utils/api-path-for :api/post-mark) mark-data))

;; ANSWERS

(defn create-answer [answer]
  (default-post-call
    (str utils/api-url
         "/api/v1/objectives/" (:objective-id answer)
         "/questions/" (:question-id answer) "/answers")
    answer))

(defn retrieve-answers
  ([question-uri]
   (retrieve-answers question-uri {}))

  ([question-uri options]
   (default-get-call
     (str utils/api-url "/api/v1" question-uri "/answers")
     {:query-params options})))

;; WRITERS

(defn create-invitation [invitation]
  (default-post-call
    (str utils/api-url "/api/v1/objectives/" (:objective-id invitation) "/writer-invitations") invitation))

(defn retrieve-invitation-by-uuid [uuid]
  (default-get-call
    (str utils/api-url "/api/v1/invitations?uuid=" uuid)))

(defn decline-invitation [{:keys [objective-id invitation-id] :as invitation}]
  (default-put-call
    (str utils/api-url "/api/v1/objectives/" objective-id "/writer-invitations/" invitation-id)
    invitation))

(defn retrieve-writers [objective-id]
  (default-get-call
    (str utils/api-url "/api/v1/objectives/" objective-id "/writers")))

(defn post-writer [writer]
  (default-post-call (str utils/api-url
                          "/api/v1/objectives/" (:objective-id writer)
                          "/writers") writer))

(defn post-profile [profile-data]
  (default-put-call (utils/api-path-for :api/put-writer-profile) profile-data))

(defn post-writer-note [note-data]
  (default-post-call (utils/api-path-for :api/post-writer-note) note-data))

;; DRAFTS

(defn get-draft [objective-id draft-id]
  (default-get-call (utils/api-path-for :api/get-draft :id objective-id :d-id draft-id)))

(defn post-draft [{objective-id :objective-id :as draft}]
  (default-post-call (utils/api-path-for :api/post-draft :id objective-id) draft))

(defn get-all-drafts [objective-id]
  (default-get-call (utils/api-path-for :api/get-drafts-for-objective :id objective-id)))

(defn get-draft-section [section-uri]
  (default-get-call (str utils/api-url "/api/v1" section-uri)))

(defn get-draft-sections [draft-uri]
  (default-get-call (str utils/api-url "/api/v1" draft-uri "/sections")))

(defn get-annotations [draft-uri]
  (default-get-call (str utils/api-url "/api/v1" draft-uri "/annotations")))

;; VOTES

(defn create-up-down-vote [vote]
  (default-post-call (utils/api-path-for :api/post-up-down-vote) vote))

;; STARS

(defn post-star [star-data]
  (default-post-call (utils/api-path-for :api/post-star) star-data))

;; ADMIN REMOVALS

(defn post-admin-removal [admin-removal-data]
  (default-post-call (utils/api-path-for :api/post-admin-removal) admin-removal-data))

(defn get-admin-removals []
  (default-get-call (utils/api-path-for :api/get-admin-removals)))

;; Promoting Objectives

(defn post-promote-objective [promoted-objective-data]
  (default-put-call (utils/api-path-for :api/put-promote-objective) promoted-objective-data))
