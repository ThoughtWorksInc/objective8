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
            [objective8.actions :as actions]))

(defn invalid-response [message]
  {:status 400
   :headers {"Content-Type" "application/json"}
   :body message})

(defn resource-locked-response [message]
  {:status 423
   :headers {"Content-Type" "application/json"}
   :body message})

(defn successful-post-response [resource-location stored-object]
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
      (successful-post-response resource-location user))
    (catch Exception e
      (log/info "Error when posting a user profile: " e)
      (invalid-response "Username must be unique"))))

(defn get-user [{:keys [route-params] :as request}]
  (let [id (-> (:id route-params)
               Integer/parseInt)]
    (if-let [user (users/retrieve-user id)]
      (-> user
          response/response
          (response/content-type "application/json"))
      (response/not-found ""))))

;; OBJECTIVE
(defn post-objective [{:keys [params] :as request}]
  (try
    (let [objective (-> params
                        (select-keys [:title :goal-1 :goal-2 :goal-3 :description :end-date :created-by-id]))
          stored-objective (objectives/store-objective! objective)
          resource-location (str utils/host-url "/api/v1/objectives/" (:_id stored-objective))]
      (successful-post-response resource-location stored-objective))
    (catch Exception e
      (log/info "Error when posting objective: " e)
      (invalid-response "Invalid objective post request"))))

(defn get-objective [{:keys [route-params] :as request}]
  (let [id (-> (:id route-params)
               Integer/parseInt)]
    (if-let [objective (objectives/retrieve-objective id)]
      (-> objective
          (update-in [:end-date] utils/date-time->iso-time-string)
          response/response
          (response/content-type "application/json"))
      (response/not-found ""))))

(defn get-objectives [_]
  (response/content-type (response/response (objectives/retrieve-objectives)) "application/json"))

;; COMMENT
(defn post-comment [{:keys [params] :as request}]
  (try
    (if-let [stored-comment (-> params
                                (select-keys [:comment :objective-id :created-by-id :comment-on-id])
                                comments/create-comment)]
      (successful-post-response (str utils/host-url "/api/v1/comments/" (:_id stored-comment))
                                stored-comment)
      (resource-locked-response "New content cannot be posted against this objective as it is now in drafting."))
    (catch Exception e
      (log/info "Error when posting comment: " e)
      (invalid-response "Invalid comment post request"))))

(defn retrieve-comments [{:keys [route-params] :as request}]
  (let [id (-> (:id route-params)
               Integer/parseInt)]
    (if-let [objective (objectives/retrieve-objective id)]
      (let [comments (comments/retrieve-comments (:global-id objective))]
        (-> comments
            response/response
            (response/content-type "application/json")))
      (response/not-found ""))))

;;QUESTIONS
(defn post-question [{:keys [route-params params] :as request}]
  (try
    (let [id (-> (:id route-params)
                 Integer/parseInt)]
      (if-let [stored-question (-> params
                                   (select-keys [:question :created-by-id])
                                   (assoc :objective-id id)
                                   questions/create-question)]
        (successful-post-response (str utils/host-url
                                       "/api/v1/objectives/" (:objective-id stored-question)
                                       "/questions/" (:_id stored-question))
                                  stored-question)
        (resource-locked-response "New content cannot be posted against this objective as it is now in drafting.")))
    (catch Exception e
      (log/info "Error when posting question: " e)
      (invalid-response "Invalid question post request"))))

(defn check-question-matches-objective [question-id objective-id]
  (let [question (questions/retrieve-question question-id)]
    (when-not (= (:objective-id question) objective-id)
      ;TODO return a 404 here, rather than throwing an exception?
      (throw (Exception. "Question does not belong to this objective")))))

(defn get-question [{:keys [route-params] :as request}]
  (try
    (let [q-id (-> (:q-id route-params)
                   Integer/parseInt)
          objective-id (-> (:id route-params)
                           Integer/parseInt)]
      (check-question-matches-objective q-id objective-id) 
      (if-let [question (questions/retrieve-question q-id)]
        (-> question
            response/response
            (response/content-type "application/json"))
        (response/not-found "")))
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
      (check-question-matches-objective q-id objective-id) 
      (let [answer (-> params
                       (select-keys [:answer :created-by-id])
                       (assoc :objective-id objective-id)
                       (assoc :question-id q-id))]
        (if-let [stored-answer (answers/create-answer! answer)]
          (successful-post-response (str utils/host-url
                                         "/api/v1/objectives/" (:objective-id stored-answer)
                                         "/questions/" (:question-id stored-answer)
                                         "/answers/" (:_id stored-answer))
                                    stored-answer)
          (resource-locked-response "New content cannot be posted against this objective as it is now in drafting."))))
    (catch Exception e
      (log/info "Error when posting answer: " e)
      (invalid-response "Invalid answer post request"))))

(defn retrieve-answers [{:keys [route-params] :as request}]
  (try
    (let  [q-id (-> (:q-id route-params)
                    Integer/parseInt)
           objective-id (-> (:id route-params)
                            Integer/parseInt)]
      (check-question-matches-objective q-id objective-id)
      (if-let [answers (answers/retrieve-answers q-id)]
        (-> answers
            response/response
            (response/content-type "application/json"))
        (response/not-found "")))
    (catch Exception e
      (log/info "Invalid route: " e)
      (invalid-response "Invalid answer request for this objective"))))

;;WRITERS
(defn post-invitation [{:keys [route-params params]}]
  (try
    (let [objective-id (-> (:id route-params)
                           Integer/parseInt)
          invitation (-> params
                         (select-keys [:writer-name :reason :invited-by-id])
                         (assoc :objective-id objective-id))]
      (if-let [stored-invitation (invitations/create-invitation! invitation)]
        (successful-post-response (str utils/host-url
                                       "/api/v1/objectives/" (:objective-id stored-invitation)
                                       "/writer-invitations/" (:_id stored-invitation)) stored-invitation)
        (resource-locked-response "New content cannot be posted against this objective as it is now in drafting.")))
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
        (successful-post-response (str utils/host-url
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
(defn post-start-drafting [{{objective-id :id} :route-params}]
  (let [updated-objective (actions/start-drafting! (Integer/parseInt objective-id))]
    (successful-post-response (str utils/host-url
                                   "/api/v1/objectives/" objective-id) updated-objective)))

(defn post-draft [{{objective-id :id} :route-params :as request}]
  (let [draft-data (ar/request->draft-data request)] 
    (if-let [draft (actions/submit-draft! draft-data)]
      (successful-post-response (utils/path-for :api/get-draft :id objective-id :d-id (str (:_id draft)))
                              draft)
      (response/not-found (str "No writer found with submitter-id " (:submitter-id draft-data) " for objective " objective-id)))))

(defn get-draft [{{:keys [id d-id]} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)]
    (if (= d-id "latest")
      (let [{status :status draft :result} (actions/retrieve-latest-draft objective-id)]
        (cond
          (= status ::actions/success)
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
  (if (some-> request
              ar/request->up-down-vote-data
              actions/cast-up-down-vote!)
    {:status 200}
    {:status 403}))
