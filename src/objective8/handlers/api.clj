(ns objective8.handlers.api
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [objective8.storage.storage :as storage]
            [objective8.objectives :as objectives]
            [objective8.comments :as comments]
            [objective8.questions :as questions]
            [objective8.answers :as answers]
            [objective8.users :as users]
            [objective8.writers :as writers]
            [objective8.invitations :as invitations]
            [objective8.drafts :as drafts]
            [objective8.utils :as utils]
            [objective8.api-requests :as ar]
            [objective8.stars :as stars]
            [objective8.actions :as actions]))

(defn error-response [status message]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body {:reason message}})

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

(defn resource-created-response [resource-location stored-object]
  {:status 201
   :headers {"Content-Type" "application/json"
             "Location" resource-location}
   :body stored-object})

(defn find-user-by-query [request]
  (let [twitter-id (get-in request [:params :twitter])]
    (if-let [user (users/find-user-by-twitter-id twitter-id)]
      (response/content-type (response/response user) "application/json")
      (response/not-found ""))))

;; USERS
(defn post-user-profile [request]
  (try
    (let [twitter-id (get-in request [:params :twitter-id])
          email-address (get-in request [:params :email-address])
          username (get-in request [:params :username])
          user (users/store-user! {:twitter-id twitter-id
                                   :email-address email-address
                                   :username username})
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

;;STARS
(defn post-star [request]
  (try
    (if-let [star-data (ar/request->star-data request)]
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
    (let [objective (ar/request->objective-data request)
          stored-objective (objectives/store-objective! objective)
          resource-location (str utils/host-url "/api/v1/objectives/" (:_id stored-objective))]
      (resource-created-response resource-location stored-objective))
    (catch Exception e
      (log/info "Error when posting objective: " e)
      (invalid-response "Invalid objective post request"))))

(defn get-objective [{:keys [route-params params] :as request}]
  (let [id (-> (:id route-params)
               Integer/parseInt)
        signed-in-id (:signed-in-id params)]
    (if-let [objective (if signed-in-id
                         (objectives/get-objective-as-signed-in-user id (Integer/parseInt signed-in-id))
                         (objectives/retrieve-objective id))]
      (-> objective
          (update-in [:end-date] utils/date-time->iso-time-string)
          response/response
          (response/content-type "application/json"))
      (response/not-found ""))))

(defn get-objectives [request]
  (if-let [query (ar/request->objectives-query request)]
    (let [objectives (objectives/get-objectives query)]
      (response/content-type (response/response objectives) "application/json"))
    (invalid-response "Invalid objectives query")))

;; COMMENT
(defn post-comment [{:keys [params] :as request}]
  (try
    (if-let [comment-data (ar/request->comment-data request)]
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
    (let [{status :status comments :result} (actions/get-comments (:uri params))]
      (cond
        (= status ::actions/success)
        (-> comments
            response/response
            (response/content-type "application/json"))

        (= status ::actions/entity-not-found)
        (not-found-response "Entity does not exist")

        :else
        (internal-server-error "Error when getting comments")))
    (catch Exception e
      (log/info "Error when getting comments: " e)
      (internal-server-error "Error when getting comments"))))

;;QUESTIONS
(defn post-question [{:keys [route-params params] :as request}]
  (try
    (let [id (-> (:id route-params)
                 Integer/parseInt)]
      (if-let [stored-question (-> params
                                   (select-keys [:question :created-by-id])
                                   (assoc :objective-id id)
                                   questions/create-question)]
        (resource-created-response (str utils/host-url
                                       "/api/v1/objectives/" (:objective-id stored-question)
                                       "/questions/" (:_id stored-question))
                                  stored-question)
        (resource-locked-response "New content cannot be posted against this objective as it is now in drafting.")))
    (catch Exception e
      (log/info "Error when posting question: " e)
      (invalid-response "Invalid question post request"))))

(defn get-question [{:keys [route-params] :as request}]
  (try
    (let [q-id (-> (:q-id route-params)
                   Integer/parseInt)
          objective-id (-> (:id route-params)
                           Integer/parseInt)]
      (if-let [question (questions/retrieve-question q-id)]
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

(defn retrieve-questions [{:keys [route-params] :as request}]
  (let [objective-id (-> (:id route-params)
                         Integer/parseInt)]
    (if-let [questions (questions/retrieve-questions objective-id)]
      (-> questions
          response/response
          (response/content-type "application/json"))
      (response/not-found ""))))

;;ANSWERS
(defn post-answer [{:keys [route-params params] :as request}]
  (try
    (let [q-id (-> (:q-id route-params)
                   Integer/parseInt)
          objective-id (-> (:id route-params)
                           Integer/parseInt)] 
      (if (-> (questions/retrieve-question q-id)
              :objective-id
              (= objective-id))
        (let [answer (-> params
                         (select-keys [:answer :created-by-id])
                         (assoc :objective-id objective-id)
                         (assoc :question-id q-id))]
          (if-let [stored-answer (answers/create-answer! answer)]
            (resource-created-response (str utils/host-url
                                            "/api/v1/objectives/" (:objective-id stored-answer)
                                            "/questions/" (:question-id stored-answer)
                                            "/answers/" (:_id stored-answer))
                                       stored-answer)
            (resource-locked-response "New content cannot be posted against this objective as it is now in drafting.")))
        (invalid-response "Invalid answer post request")))
    (catch Exception e
      (log/info "Error when posting answer: " e)
      (invalid-response "Invalid answer post request"))))

(defn get-answers [{:keys [route-params] :as request}]
  (try
    (let  [q-id (-> (:q-id route-params)
                    Integer/parseInt)
           objective-id (-> (:id route-params)
                            Integer/parseInt)]
      (if (-> (questions/retrieve-question q-id)
              :objective-id
              (= objective-id))
        (-> (answers/get-answers q-id)
            response/response
            (response/content-type "application/json")) 
        (not-found-response "Question does not exist")))
    (catch Exception e
      (log/info "Invalid route: " e)
      (invalid-response "Invalid answer request for this objective"))))

;;WRITERS
(defn post-invitation [request]
  (try
    (let [invitation-data (ar/request->invitation-data request)
          {status :status stored-invitation :result} (actions/create-invitation! invitation-data)]
      (cond
        (= status ::actions/success)

        (resource-created-response (str utils/host-url
                                        "/api/v1/objectives/" (:objective-id stored-invitation)
                                        "/writer-invitations/" (:_id stored-invitation)) stored-invitation)

        (= status ::actions/objective-drafting-started)
        (resource-locked-response "New content cannot be posted against this objective as it is now in drafting.")

        (= status ::actions/entity-not-found)
        (not-found-response "Entity does not exist")

        :else
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

(defn post-candidate-writer [{{objective-id :id} :route-params
                              params :params :as request}]
  (try
    (let [candidate-data (ar/request->candidate-data request)]
      (if-let [{candidate-id :_id :as candidate} (writers/create-candidate candidate-data)]
        (resource-created-response (str utils/host-url
                                       "/api/v1/objectives/" objective-id
                                       "/candidate-writers/" candidate-id) candidate)
        {:status 403}))
    (catch Exception e
      (log/info "Error when creating candidate: " e)
      (invalid-response (str "Error when creating candidate writer")))))

(defn retrieve-candidates [{:keys [route-params params]}]
  (try
    (let [objective-id (-> (:id route-params)
                           Integer/parseInt)
          candidates (writers/retrieve-candidates objective-id)]
      (-> candidates
          response/response
          (response/content-type "application/json")))
    (catch Exception e
      (log/info "Error when retrieving candidates: " e)
      (invalid-response "Invalid candidates get request for this objective"))))

;;DRAFTS
(defn post-draft [{{objective-id :id} :route-params :as request}]
  (let [draft-data (ar/request->draft-data request)] 
    (if-let [draft (actions/submit-draft! draft-data)]
      (resource-created-response (utils/path-for :api/get-draft :id objective-id :d-id (str (:_id draft)))
                              draft)
      (response/not-found (str "No writer found with submitter-id " (:submitter-id draft-data) " for objective " objective-id)))))

(defn get-draft [{{:keys [id d-id]} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)]
    (if (= d-id "latest")
      (let [{status :status draft :result} (actions/retrieve-latest-draft objective-id)]
        (cond
          (and (= status ::actions/success) draft)
          (-> draft
              response/response
              (response/content-type "application/json"))

          (= status ::actions/objective-drafting-not-started)
          {:status 403}

          :else
          (response/not-found "")))
      
    (if-let [draft (drafts/retrieve-draft (Integer/parseInt d-id))]
      (-> draft
          response/response
          (response/content-type "application/json"))
      (response/not-found "")))))

(defn retrieve-drafts [{{:keys [id]} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)
        {status :status drafts :result} (actions/retrieve-drafts objective-id)]
    (cond
      (= status ::actions/success)
      (-> drafts
          response/response
          (response/content-type "application/json"))

      (= status ::actions/objective-drafting-not-started)
      {:status 403}

      :else
      (response/not-found ""))))

(defn post-up-down-vote [request]
  (try
    (if-let [vote-data (ar/request->up-down-vote-data request)]
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

(defn post-pin [request]
 (try
   (if-let [pin-data (ar/request->pin-data request)]
     (let [{status :status pin :result} (actions/pin-question! pin-data)]
       (cond
          (= status ::actions/success)
          (resource-created-response (str utils/host-url
                                       "/api/v1" (:uri pin) 
                                       ) pin)

          (= status ::actions/forbidden)
          (forbidden-response (str "Cannot pin question " (:question-uri pin-data)))

          (= status ::actions/entity-not-found)
          (not-found-response (str "Entity with uri " (:question-uri pin-data) " does not exist"))
          
          :else
          (internal-server-error "Error when pinning question")))
    (invalid-response "Invalid pin request"))
   (catch Exception e
     (log/info "Error when pinning question: " e)
     (internal-server-error "Error when pinning question"))))
