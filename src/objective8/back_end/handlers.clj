(ns objective8.back-end.handlers
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [objective8.utils :as utils]
            [objective8.back-end.storage.storage :as storage]
            [objective8.back-end.domain.objectives :as objectives]
            [objective8.back-end.domain.comments :as comments]
            [objective8.back-end.domain.questions :as questions]
            [objective8.back-end.domain.answers :as answers]
            [objective8.back-end.domain.users :as users]
            [objective8.back-end.domain.writers :as writers]
            [objective8.back-end.domain.invitations :as invitations]
            [objective8.back-end.domain.drafts :as drafts]
            [objective8.back-end.requests :as br]
            [objective8.back-end.domain.stars :as stars]
            [objective8.back-end.domain.admin-removals :as admin-removals]
            [objective8.back-end.actions :as actions]
            [objective8.back-end.domain.activities :as activities]))

(defn error-response [status message]
  {:status  status
   :headers {"Content-Type" "application/json"}
   :body    {:reason message}})

(defn invalid-response [message]
  (error-response 400 message))

(defn resource-locked-response [message]
  (error-response 423 message))

(defn forbidden-response [message]
  (error-response 403 message))

(defn internal-server-error [message]
  (error-response 500 message))

(defn not-found-response [message]
  (error-response 404 message))

(defn ok-response []
  {:status 200})

(defn resource-updated-response [resource-location updated-object]
  {:status  200
   :headers {"Content-Type" "application/json"
             "Location"     resource-location}
   :body    updated-object})

(defn resource-created-response [resource-location stored-object]
  {:status  201
   :headers {"Content-Type" "application/json"
             "Location"     resource-location}
   :body    stored-object})

;; USERS

(defn find-user-by-auth-provider-user-id [auth-provider-user-id]
  (if-let [user (users/find-user-by-auth-provider-user-id auth-provider-user-id)]
    (response/content-type (response/response user) "application/json")
    (response/not-found "")))

(defn find-user-by-username [username]
  (if-let [user (users/find-user-by-username username)]
    (response/content-type (response/response user) "application/json")
    (response/not-found "")))

(defn find-user-by-query [{:keys [params] :as request}]
  (if-let [auth-provider-user-id (params :auth_provider_user_id)]
    (find-user-by-auth-provider-user-id auth-provider-user-id)
    (if-let [username (params :username)]
      (find-user-by-username username))))

(defn post-user-profile [request]
  (try
    (let [user-data (select-keys (:params request) [:auth-provider-user-id :email-address :username])
          user (users/store-user! user-data)
          resource-location (str utils/host-url "/api/v1/users/" (:_id user))]
      (resource-created-response resource-location user))
    (catch Exception e
      (log/info "Error when posting a user profile: " e)
      (invalid-response "Username must be unique"))))

(defn get-user [{:keys [route-params] :as request}]
  (let [{status :status user :result} (actions/get-user-with-roles (str "/users/" (:id route-params)))]
    (cond
      (= status ::actions/success)
      (-> user
          response/response
          (response/content-type "application/json"))
      (= status ::actions/entity-not-found)
      (response/not-found "")
      :else
      (internal-server-error "Error when getting user"))))

(defn put-writer-profile [request]
  (try
    (if-let [profile-data (br/request->profile-data request)]
      (let [{status :status user :result} (actions/update-user-with-profile! profile-data)]
        (cond
          (= status ::actions/success)
          (resource-updated-response (utils/api-path-for :api/get-user :id (:_id user)) user)

          (= status ::actions/entity-not-found)
          (not-found-response "User does not exist with that user-uri")

          :else
          (internal-server-error "Error when posting profile")))
      (invalid-response "Invalid profile post request"))
    (catch Exception e
      (log/info "Error when posting profile: " e)
      (internal-server-error "Error when posting profile"))))

;; ADMINS

(defn post-admin-removal [request]
  (try
    (if-let [admin-removal-data (br/request->admin-removal-data request)]
      (let [{status :status admin-removal :result} (actions/create-admin-removal! admin-removal-data)]
        (cond
          (= status ::actions/success)
          (resource-created-response (str utils/host-url "/api/v1/meta/admin-removals/" (:_id admin-removal))
                                     admin-removal)

          (= status ::actions/entity-not-found)
          (not-found-response "Entity with that uri does not exist for removal")

          (= status ::actions/forbidden)
          (forbidden-response "Admin credentials are required for this request")

          :else
          (internal-server-error "Error when posting admin-removal")))
      (invalid-response "Invalid admin-removal post request"))
    (catch Exception e
      (log/info "Error when posting admin-removal: " e)
      (internal-server-error "Error when posting admin-removal"))))

(defn get-admin-removals [_]
  (try
    (-> (admin-removals/get-admin-removals)
        response/response
        (response/content-type "application/json"))
    (catch Exception e
      (log/info "Error when getting admin-removals: " e)
      (internal-server-error "Error when getting admin-removals"))))

;; PROMOTED OBJECTIVES

(defn post-promote-objective [request]
  (try
    (if-let [pin-data (br/request->promote-objective-data request)]
      (let [{status :status promoted-objective :result} (actions/create-promote-objective! pin-data)]
        (cond
          (= status ::actions/success)
          (resource-updated-response (str utils/host-url "/api/v1/meta/promote-objective/" (:_id promoted-objective))
                                     promoted-objective)
          (= status ::actions/forbidden)
          (forbidden-response "Admin credentials are required for this request. Maximum 3 objectives may be promoted.")

          (= status ::actions/entity-not-found)
          (not-found-response "Objective with that URI does not exist")

          :else
          (internal-server-error "Error when posting to promote objective")))
      (invalid-response "Invalid promote objective post request"))
    (catch Exception e
      (log/info "Error when promoting objective: " e)
      (internal-server-error "Error when promoting objective"))))


;;STARS
(defn post-star [request]
  (try
    (if-let [star-data (br/request->star-data request)]
      (let [{status :status star :result} (actions/toggle-star! star-data)]
        (cond
          (= status ::actions/success)
          (resource-created-response (str utils/host-url "/api/v1/meta/stars/" (:_id star))
                                     star)

          (= status ::actions/entity-not-found)
          (not-found-response "Objective does not exist")

          :else
          (internal-server-error "Error when posting star")))
      (invalid-response "Invalid star post request"))
    (catch Exception e
      (log/info "Error when posting star: " e)
      (internal-server-error "Error when posting star"))))

;; OBJECTIVES
(defn post-objective [request]
  (try
    (let [objective (br/request->objective-data request)
          {status :status stored-objective :result} (actions/create-objective! objective)]
      (if (= status ::actions/success)
        (resource-created-response (str utils/host-url "/api/v1/objectives/" (:_id stored-objective))
                                   stored-objective)
        (internal-server-error "Error when posting objective")))
    (catch Exception e
      (log/info "Error when posting objective: " e)
      (invalid-response "Invalid objective post request"))))

(defn get-objective [{:keys [route-params params] :as request}]
  (let [id (-> (:id route-params)
               Integer/parseInt)
        signed-in-id (:signed-in-id params)
        include-removed? (:include-removed params)]
    (if-let [objective (cond
                         signed-in-id
                         (objectives/get-objective-as-signed-in-user id (Integer/parseInt signed-in-id))

                         :else
                         (objectives/get-objective id include-removed?))]
      (-> objective
          response/response
          (response/content-type "application/json"))
      (response/not-found ""))))

(defn get-objectives [request]
  (if-let [query (br/request->objectives-query request)]
    (let [objectives (objectives/get-objectives query)]
      (response/content-type (response/response objectives) "application/json"))
    (invalid-response "Invalid objectives query")))

;; COMMENT
(defn post-comment [{:keys [params] :as request}]
  (try
    (if-let [comment-data (br/request->comment-data request)]
      (let [{status :status comment :result} (actions/create-comment! comment-data)]
        (cond
          (= status ::actions/success)
          (resource-created-response (str utils/host-url "/api/v1/meta/comments/" (:_id comment))
                                     comment)

          (= status ::actions/entity-not-found)
          (not-found-response "Entity does not exist")

          :else
          (internal-server-error "Error when posting comment")))
      (invalid-response "Invalid comment post request"))
    (catch Exception e
      (log/info "Error when posting comment: " e)
      (internal-server-error "Error when posting comment"))))

(defn get-comments [{:keys [params] :as request}]
  (try
    (if-let [{uri :uri :as query} (br/request->comments-query request)]
      (if-let [comments-data (comments/get-comments-with-pagination-data uri (dissoc query :uri))]
        (-> comments-data
            response/response
            (response/content-type "application/json"))
        (not-found-response "Entity does not exist"))
      (invalid-response "Invalid request for comments"))
    (catch Exception e
      (log/info "Error when getting comments: " e)
      (internal-server-error "Error when getting comments"))))

;;QUESTIONS
(defn post-question [{:keys [route-params params] :as request}]
  (try
    (let [id (-> (:id route-params)
                 Integer/parseInt)
          {:keys [status result] :as stored-question} (-> params
                                                          (select-keys [:question :created-by-id])
                                                          (assoc :objective-id id)
                                                          actions/create-question!)]
      (case status
        ::actions/success
        (resource-created-response (str utils/host-url
                                        "/api/v1/objectives/" (:objective-id result)
                                        "/questions/" (:_id result))
                                   result)
        ::actions/entity-not-found
        (not-found-response "The objective does not exist")

        (internal-server-error "Error when posting question")))
    (catch Exception e
      (log/info "Error when posting question: " e)
      (invalid-response "Invalid question post request"))))

(defn get-question [{:keys [route-params] :as request}]
  (try
    (let [q-id (-> (:q-id route-params)
                   Integer/parseInt)
          objective-id (-> (:id route-params)
                           Integer/parseInt)
          question-uri (str "/objectives/" objective-id "/questions/" q-id)]
      (if-let [question (questions/get-question question-uri)]
        (if (-> (:objective-id question)
                (= objective-id))
          (-> question
              response/response
              (response/content-type "application/json"))
          (not-found-response "Question does not belong to this objective"))
        (not-found-response "Question does not exist")))
    (catch Exception e
      (log/info "Invalid route: " e)
      (invalid-response "Invalid question request for this objective"))))

(defn retrieve-questions [{:keys [route-params params] :as request}]
  (let [objective-id (-> (:id route-params)
                         Integer/parseInt)
        objective-uri (str "/objectives/" objective-id)]
    (if-let [questions (if (= (:sorted-by params) "answers")
                         (questions/get-questions-for-objective-by-most-answered objective-uri)
                         (questions/get-questions-for-objective objective-uri))]
      (-> questions
          response/response
          (response/content-type "application/json"))
      (response/not-found ""))))

;;ANSWERS
(defn post-answer [{:keys [route-params params] :as request}]
  (try
    (if-let [answer-data (br/request->answer-data request)]
      (let [{status :status answer :result} (actions/create-answer! answer-data)]
        (case status
          ::actions/success
          (resource-created-response (str utils/host-url
                                          "/api/v1/objectives/" (:objective-id answer)
                                          "/questions/" (:question-id answer)
                                          "/answers/" (:_id answer))
                                     answer)

          ::actions/entity-not-found
          (not-found-response "Question does not exist")

          ;; default
          (internal-server-error "Error when posting comment")))
      (invalid-response "Invalid answer post request"))
    (catch Exception e
      (log/info "Error when posting answer: " e)
      (invalid-response "Invalid answer post request"))))

(defn get-answers [{:keys [route-params params] :as request}]
  (try
    (if-let [{question-uri :question-uri sorted-by :sorted-by filter-type :filter-type :as query-params} (br/request->answers-query request)]
      (if (questions/get-question question-uri)
        (let [answers-query {:sorted-by   sorted-by
                             :filter-type filter-type
                             :offset      (get query-params :offset 0)}
              answers (answers/get-answers question-uri answers-query)]
          (-> answers
              response/response
              (response/content-type "application/json")))
        (not-found-response "Question does not exist"))
      (invalid-response "Invalid answers query"))
    (catch Exception e
      (log/info "Error when retrieving answers: " e)
      (invalid-response "Invalid answer request for this objective"))))

;;WRITER-NOTES
(defn post-writer-note [request]
  (try
    (let [writer-note-data (br/request->writer-note-data request)
          {status :status stored-note :result} (actions/create-writer-note! writer-note-data)]
      (cond
        (= status ::actions/success)
        (resource-created-response (str utils/host-url
                                        "/api/v1/meta/writer-notes/" (:_id stored-note)) stored-note)

        (= status ::actions/forbidden)
        (forbidden-response (str "Cannot post a note against entity"))

        (= status ::actions/entity-not-found)
        (not-found-response "Entity does not exist")
        ))))

;;WRITERS
(defn post-invitation [request]
  (try
    (let [invitation-data (br/request->invitation-data request)
          {status :status stored-invitation :result} (actions/create-invitation! invitation-data)]
      (case status
        ::actions/success (resource-created-response
                            (str utils/host-url
                                 "/api/v1/objectives/" (:objective-id stored-invitation)
                                 "/writer-invitations/" (:_id stored-invitation)) stored-invitation)

        ::actions/entity-not-found (not-found-response "Entity does not exist")

        (internal-server-error "Error when posting invitation")))
    (catch Exception e
      (log/info "Error when posting an invitation: " e)
      (invalid-response "Invalid invitation post request for this objective"))))

(defn get-invitation [{{uuid :uuid} :params}]
  (try
    (if-let [invitation (invitations/get-invitation uuid)]
      (-> invitation
          response/response
          (response/content-type "application/json"))
      (response/not-found (str "No active invitations exist with uuid: " uuid)))
    (catch Exception e
      (log/info "Error when retrieving invitation: " e)
      (invalid-response (str "Error when retrieving invitation with uuid " uuid)))))

(defn put-invitation-declination [{{uuid :invitation-uuid} :params}]
  (if-let [declined-invitation (invitations/decline-invitation-by-uuid uuid)]
    (-> declined-invitation
        response/response
        (response/content-type "application/json"))
    {:status 404}))

(defn post-writer [{{objective-id :id} :route-params
                    params             :params :as request}]
  (try
    (let [writer-data (br/request->writer-data request)]
      (if-let [{writer-id :_id :as writer} (writers/create-writer writer-data)]
        (resource-created-response (str utils/host-url
                                        "/api/v1/objectives/" objective-id
                                        "/writers/" writer-id) writer)
        {:status 403}))
    (catch Exception e
      (log/info "Error when creating writer: " e)
      (invalid-response (str "Error when creating writer")))))

(defn retrieve-writers [{:keys [route-params params]}]
  (try
    (let [objective-id (-> (:id route-params)
                           Integer/parseInt)
          writers (writers/retrieve-writers objective-id)]
      (-> writers
          response/response
          (response/content-type "application/json")))
    (catch Exception e
      (log/info "Error when retrieving writers: " e)
      (invalid-response "Invalid writers get request for this objective"))))

;;WRITER-OBJECTIVES

(defn get-objectives-for-writer [{:keys [route-params]}]
  (try
    (let [user-id (-> (:id route-params)
                      Integer/parseInt)
          objectives (objectives/get-objectives-for-writer user-id)]
      (-> objectives
          response/response
          (response/content-type "application/json")))
    (catch Exception e
      (log/info "Errow when retrieving objectives for writer: " e)
      (invalid-response "Invalid objectives get request for this writer"))))

;;DRAFTS
(defn post-draft [{{objective-id :id} :route-params :as request}]
  (let [draft-data (br/request->draft-data request)]
    (if-let [draft (actions/submit-draft! draft-data)]
      (resource-created-response (utils/api-path-for :api/get-draft :id objective-id :d-id (str (:_id draft)))
                                 draft)
      (response/not-found (str "No writer found with submitter-id " (:submitter-id draft-data) " for objective " objective-id)))))

(defn get-draft [{{:keys [id d-id]} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)]
    (if (= d-id "latest")
      (if-let [draft (drafts/retrieve-latest-draft objective-id)]
        (-> draft
            response/response
            (response/content-type "application/json"))
        (response/not-found ""))

      (if-let [draft (drafts/retrieve-draft-with-comment-count (Integer/parseInt d-id))]
        (-> draft
            response/response
            (response/content-type "application/json"))
        (response/not-found "")))))

(defn retrieve-drafts [{{:keys [id]} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)]
    (if-let [drafts (drafts/retrieve-drafts objective-id)]
      (-> drafts
          response/response
          (response/content-type "application/json"))
      (response/not-found ""))))

;;SECTION
(defn get-section [{:keys [route-params] :as request}]
  (try
    (let [section-label (:section-label route-params)
          draft-id (-> (:d-id route-params)
                       Integer/parseInt)
          objective-id (-> (:id route-params)
                           Integer/parseInt)
          section-uri (str "/objectives/" objective-id "/drafts/" draft-id "/sections/" section-label)]
      (if-let [section (drafts/get-section section-uri)]
        (if (-> (:objective-id section)
                (= objective-id))
          (-> section
              response/response
              (response/content-type "application/json"))
          (not-found-response "Draft does not belong to this objective"))
        (not-found-response "Section does not exist")))
    (catch Exception e
      (log/info "Invalid route: " e)
      (invalid-response "Invalid section request for this draft"))))

(defn get-sections [{:keys [route-params] :as request}]
  (-> (str "/objectives/" (:id route-params) "/drafts/" (:d-id route-params))
      drafts/get-draft-sections-with-annotation-count
      response/response
      (response/content-type "application/json")))

(defn get-annotations [{:keys [route-params] :as request}]
  (let [draft-uri (str "/objectives/" (:id route-params) "/drafts/" (:d-id route-params))
        {status :status annotations :result} (actions/get-annotations-for-draft draft-uri)]
    (if (= status ::actions/success)
      (-> annotations
          response/response
          (response/content-type "application/json"))
      (response/not-found ""))))

(defn post-up-down-vote [request]
  (try
    (if-let [vote-data (br/request->up-down-vote-data request)]
      (let [{status :status vote :result} (actions/cast-up-down-vote! vote-data)]
        (cond
          (= status ::actions/success)
          (ok-response)

          (= status ::actions/forbidden)
          (forbidden-response (str "Cannot post a vote against entity at " (:uri vote-data)))

          (= status ::actions/entity-not-found)
          (not-found-response (str "Entity with uri " (:uri vote-data) " does not exist"))

          :else
          (internal-server-error "Error when posting vote")))

      (invalid-response "Invalid vote post request"))
    (catch Exception e
      (log/info "Error when posting vote: " e)
      (internal-server-error "Error when posting vote"))))

(defn post-mark [request]
  (try
    (if-let [mark-data (br/request->mark-data request)]
      (let [{status :status mark :result} (actions/mark-question! mark-data)]
        (cond
          (= status ::actions/success)
          (resource-created-response (str utils/host-url
                                          "/api/v1" (:uri mark)
                                          ) mark)

          (= status ::actions/forbidden)
          (forbidden-response (str "Cannot mark question " (:question-uri mark-data)))

          (= status ::actions/entity-not-found)
          (not-found-response (str "Entity with uri " (:question-uri mark-data) " does not exist"))

          :else
          (internal-server-error "Error when marking question")))
      (invalid-response "Invalid mark request"))
    (catch Exception e
      (log/info "Error when marking question: " e)
      (internal-server-error "Error when marking question"))))
