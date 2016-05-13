(ns objective8.front-end.handlers
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.util.response :as response]
            [objective8.front-end.config :as fe-config]
            [objective8.front-end.api.http :as http-api]
            [objective8.front-end.front-end-requests :as fr]
            [objective8.utils :as utils]
            [objective8.front-end.permissions :as permissions]
            [objective8.front-end.draft-diffs :as diffs]
            [objective8.front-end.views :as views]
            [objective8.config :as config]))

(declare accept-invitation)

;; HELPERS

(defn signed-in? []
  (friend/authorized? #{:signed-in} friend/*identity*))

(defn error-404-response [request]
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body (views/error-404 "error-404" request)})

(defn- redirect-to-params-referer
  ([request]
   (redirect-to-params-referer request nil))

  ([request fragment]
   (let [location (get-in request [:params :refer] "/")
         redirect-url (utils/safen-url (str location (when fragment (str "#" fragment))))]
     (response/redirect redirect-url))))

;; HANDLERS

(defn error-404 [request]
  (error-404-response request))

(defn error-configuration [request]
  {:status 500
   :headers {"Content-Type" "text/html"}
   :body (views/error-configuration "error-configuration" request)})

(defn default-error-page [request error-status]
  {:status error-status
   :headers {"Content-Type" "text/html"}
   :body (views/error-default "error-default" request)})

(defn error-log-in [request]
  {:status 500
   :headers {"Content-Type" "text/html"}
   :body (views/error-log-in "error-log-in" request)})

(defn index [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (views/index "index" request)})

(defn sign-in [{{refer :refer} :params :as request}]
  (let [unauthorised-uri (get-in request [:session :cemerick.friend/unauthorized-uri])
        referrer (or refer (utils/uri->route unauthorised-uri))]
    {:status  200
     :header  {"Content-Type" "text/html"}
     :body    (views/sign-in "sign-in" request)
     :session (assoc (:session request) :sign-in-referrer referrer)}))

(defn authorisation-page [request]
  {:status  403
   :headers {"Content-Type" "text/html"}
   :body    (views/authorisation-page "authorisation-page" request)})

(defn sign-out [request]
  (assoc
    (friend/logout* (response/redirect "/"))
    :session {}))

(defn project-status [{:keys [t' locale] :as request}]
  {:status 200
   :header {"Content-Type" "text/html"}
   :body (views/project-status "project-status" request)})

(defn learn-more [request]
  {:status 200
   :header {"Content-Type" "text/html"}
   :body (views/learn-more-page "learn-more" request)})

(defn cookies [request]
  (if (:cookie-message-enabled config/environment)
    {:status  200
     :headers {"Content-Type" "text/html"}
     :body    (views/cookies "cookie-page" request)}
    (error-404-response request)))

;; USER PROFILE

(defn profile [{:keys [route-params] :as request}]
  (let [username (:username route-params)
        {user-status :status user :result} (http-api/find-user-by-username username)]
    (case user-status
      ::http-api/success
      (let [user-profile (:profile user)
            {objective-status :status  objectives-for-writer :result} (http-api/get-objectives-for-writer (:_id user))
            joined-date (utils/iso-time-string->pretty-date (:_created_at user))]
        (if (= objective-status ::http-api/success)
          {:status 200
           :header {"Content-Type" "text/html"}
           :body   (views/profile "profile" request
                                  :user-profile user-profile
                                  :profile-owner username
                                  :objectives-for-writer objectives-for-writer
                                  :joined-date joined-date
                                  :doc {:title (str (:name user-profile) " | " (:app-name config/environment))})}
          (error-404-response request)))

      ::http-api/not-found
      (error-404-response request)

      (do (log/info (str "Error when retrieving user by name " {:http-api-status user-status :username username}))
          (default-error-page request 500)))))


;; OBJECTIVES
(defn objective-list [request]
  (let [signed-in-id (get (friend/current-authentication) :identity)
        {status :status objectives :result :as api-call} (if signed-in-id
                                              (http-api/get-objectives {:signed-in-id signed-in-id})
                                              (http-api/get-objectives))]
    (cond
      (= status ::http-api/success)
      {:status 200
       :header {"Content-Type" "text/html"}
       :body (views/objective-list "objective-list" request
                                   :objectives objectives)}

      (= status ::http-api/error)
      (do (log/info (str "Error when retrieving objectives " {:http-api-status status}))
          (default-error-page request 502)))))

(defn create-objective-form [request]
  {:status 200
   :header {"Content-Type" "text/html"}
   :body (views/create-objective "create-objective" request)})

(defn create-objective-form-post [{:keys [t' locale session] :as request}]
  (let [objective-data (fr/request->objective-data request (get (friend/current-authentication) :identity))]
    (case (:status objective-data)

      ::fr/valid
      (let [{status :status stored-objective :result} (http-api/create-objective (:data objective-data))]
        (cond (= status ::http-api/success)
              (let [objective-url (str utils/host-url "/objectives/" (:_id stored-objective))]
                (-> (response/redirect objective-url)
                    (assoc :flash {:type :share-objective
                                   :created-objective stored-objective}
                           :session session)
                    (permissions/add-authorisation-role (permissions/writer-for (:_id stored-objective)))
                    (permissions/add-authorisation-role (permissions/writer-inviter-for (:_id stored-objective)))))
              (= status ::http-api/invalid-input)
              (do (log/info (str "Invalid input when creating objective " {:http-api-status status
                                                                            :posted-data objective-data}))
                  (default-error-page request 400))

              :else
              (do (log/info (str "Error when creating objective " {:http-api-status status
                                                                    :posted-data objective-data}))
                  (default-error-page request 502))))

      ::fr/invalid
      (-> (response/redirect (utils/path-for :fe/create-objective-form))
          (assoc :flash {:validation (dissoc objective-data :status)})))))

(defn remove-invitation-from-session [response]
  (update-in response [:session] dissoc :invitation))

(defn update-session-invitation [{:keys [session] :as request}]
  (if-let [invitation-rsvp (:invitation session)]
    (let [invitation-status (:status (http-api/retrieve-invitation-by-uuid (:uuid invitation-rsvp)))]
      (if (= ::http-api/success invitation-status)
        request
        (remove-invitation-from-session request)))
    request))

(defn objective-detail [{:keys [params route-params] :as request}]
  (try (let [objective-id (Integer/parseInt (:id route-params))
             updated-request (update-session-invitation request)
             signed-in-id (get (friend/current-authentication) :identity)
             {objective-status :status objective :result} (if signed-in-id
                                                            (http-api/get-objective objective-id {:signed-in-id signed-in-id})
                                                            (http-api/get-objective objective-id))
             {writers-status :status writers :result} (http-api/retrieve-writers objective-id)
             {questions-status :status questions :result} (http-api/retrieve-questions objective-id)
             {comments-status :status comments-data :result} (http-api/get-comments
                                                              (:uri objective)
                                                              {:limit fe-config/comments-pagination})]
         (cond
           (every? #(= ::http-api/success %) [objective-status writers-status questions-status comments-status])
           (let [{drafts-status :status latest-draft :result} (when (> (get-in objective [:meta :drafts-count] 0) 0)
                                                                (http-api/get-draft objective-id "latest"))]
             {:status 200
              :headers {"Content-Type" "text/html"}
              :body
              (views/objective-detail-page "objective-view"
                                           updated-request
                                           :objective objective
                                           :writers writers
                                           :questions questions
                                           :comments-data comments-data
                                           :latest-draft latest-draft
                                           :doc (let [details (str (:title objective) " | " (:app-name config/environment))]
                                                  {:title details
                                                   :description details}))})
           (= objective-status ::http-api/not-found) (error-404-response updated-request)

           (= objective-status ::http-api/invalid-input)
           (do (log/info (str "Error when getting objective " {:http-api-status objective-status}))
               (default-error-page request 400))

           :else
           (do (log/info (str "Error when getting objective " {:http-api-status objective-status}))
               (default-error-page request 500))))

       (catch Exception e
         (do (log/info "Exception in objective-detail handler: " e)
             (default-error-page request 500)))))

(def dashboard-questions-answer-view-query-params
  {:up-votes {:sorted-by "up-votes" :filter-type "none"}
   :down-votes {:sorted-by "down-votes" :filter-type "none"}
   :paperclip {:sorted-by "up-votes" :filter-type "has-writer-note"}})

(defn dashboard-questions [{:keys [route-params params] :as request}]
  (let [objective-id (Integer/parseInt (:id route-params))
        {objective-status :status objective :result} (http-api/get-objective objective-id)
        {questions-status :status questions :result} (http-api/retrieve-questions objective-id {:sorted-by "answers"})

        selected-question-uri (get params :selected (:uri (first questions)))
        answer-view-type (keyword (get params :answer-view "up-votes"))
        answer-query-params (get dashboard-questions-answer-view-query-params
                                 answer-view-type
                                 {:sorted-by "up-votes" :filter-type "none"})

        {answers-status :status answers :result} (if (empty? questions)
                                                   {:status ::http-api/success :result []}
                                                   (http-api/retrieve-answers selected-question-uri answer-query-params))]
    (cond
      (every? #(= ::http-api/success %) [objective-status questions-status answers-status])
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (views/dashboard-questions-page "dashboard-questions"
                                             request
                                             :objective objective
                                             :questions questions
                                             :answers answers
                                             :answer-view-type answer-view-type
                                             :selected-question-uri selected-question-uri)}
      :else
      (do (log/info (str "Error in dashboard-questions handler " {:objective-http-api-status objective-status
                                                                   :questions-http-api-status questions-status
                                                                   :answers-http-api-status answers-status}))
          (default-error-page request 500)))))

(def dashboard-comments-query-params
  {:up-votes {:sorted-by "up-votes" :filter-type "none"}
   :down-votes {:sorted-by "down-votes" :filter-type "none"}
   :paperclip {:sorted-by "up-votes" :filter-type "has-writer-note"}})

(defn dashboard-comments [{:keys [route-params params] :as request}]
  (let [objective-id (Integer/parseInt (:id route-params))
        offset (Integer/parseInt (get params :offset "0"))
        {objective-status :status objective :result} (http-api/get-objective objective-id)
        {drafts-status :status drafts :result} (http-api/get-all-drafts objective-id)

        selected-comment-target-uri (get params :selected (:uri objective))
        comment-view-type (keyword (get params :comment-view "up-votes"))
        comment-query-params (-> (get dashboard-comments-query-params
                                          comment-view-type
                                          {:sorted-by "up-votes" :filter-type "none"})
                                 (assoc :limit fe-config/comments-pagination
                                        :offset offset))

        {comments-status :status comments-data :result} (http-api/get-comments selected-comment-target-uri
                                                                               comment-query-params)]
    (cond
      (every? #(= ::http-api/success %) [objective-status comments-status drafts-status])
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (views/dashboard-comments-page "dashboard-comments"
                                            request
                                            :objective objective
                                            :drafts drafts
                                            :comments-data comments-data
                                            :comment-view-type comment-view-type
                                            :offset offset
                                            :selected-comment-target-uri selected-comment-target-uri)}

      :else
      (do (log/info (str "Error in dashboard-comments handler " {:objective-http-api-status objective-status
                                                                  :comments-http-api-status comments-status
                                                                  :drafts-http-api-status drafts-status}))
          (default-error-page request 500)))))

(defn dashboard-annotations [{:keys [route-params params] :as request}]
  (let [objective-id (Integer/parseInt (:id route-params))
        {objective-status :status objective :result} (http-api/get-objective objective-id)
        {drafts-status :status drafts :result} (http-api/get-all-drafts objective-id)

        selected-draft-uri (get params :selected (:uri (first drafts)))
        {annotations-status :status annotations :result} (http-api/get-annotations selected-draft-uri)]

    (cond
      (every? #(= ::http-api/success %) [objective-status annotations-status])
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (views/dashboard-annotations-page "dashboard-annotations"
                                               request
                                               :objective objective
                                               :drafts drafts
                                               :annotations annotations
                                               :selected-draft-uri selected-draft-uri)}

      (and (= objective-status ::http-api/success) (= annotations-status ::http-api/not-found))
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (views/dashboard-annotations-page "dashboard-annotations"
                                               request
                                               :objective objective
                                               :drafts drafts
                                               :annotations nil
                                               :selected-draft-uri selected-draft-uri)}
      :else
      (do (log/info (str "Error in dashboard-annotations handler " {:objective-http-api-status objective-status
                                                                     :annotations-http-api-status annotations-status
                                                                     :drafts-http-api-status drafts-status}))
          (default-error-page request 500)))))

(defn post-writer-note [{:keys [route-params params] :as request}]
  (if-let [writer-note-data (fr/request->writer-note-data request (get (friend/current-authentication) :identity))]
    (case (:status writer-note-data)
      ::fr/valid
      (let [{status :status} (http-api/post-writer-note (:data writer-note-data))]
        (cond
          (= status ::http-api/success)
          (redirect-to-params-referer request)

          (= status ::http-api/forbidden)
          (do (log/info (str "Attempt to post writer note when not authorised " {:data writer-note-data}))
              (default-error-page request 403))))

      ::fr/invalid
      (-> (redirect-to-params-referer request)
          (assoc :flash {:validation (dissoc writer-note-data :status)})))

    (do (log/info (str "Invalid data when creating writer note " {:request-params params}))
        (default-error-page request 400))))

;; COMMENTS

(defn get-comments-for-objective [{:keys [route-params params t'] :as request}]
  (try
    (let [objective-id (Integer/parseInt (:id route-params))
          {objective-status :status objective :result} (http-api/get-objective objective-id)
          comment-count (get-in objective [:meta :comments-count])
          offset (Integer/parseInt (get params :offset "0"))
          comments-query-params {:offset offset
                                 :limit fe-config/comments-pagination}]
      (if (= objective-status ::http-api/success)
        (if (< offset 0)
          (response/redirect (utils/path-for :fe/get-comments-for-objective
                                             :id objective-id))
          (if (or (< offset comment-count)
                  (and (= offset 0) (= comment-count 0)))
            (let [{comments-status :status comments-data :result} (http-api/get-comments
                                                                    (:uri objective)
                                                                    comments-query-params)]
              (if (= comments-status ::http-api/success)
                {:status 200
                 :headers {"Content-Type" "text/html"}
                 :body (views/objective-comments-view
                         "objective-comments"
                         request
                         :objective objective
                         :comments-data comments-data
                         :doc (let [details (str (t' :objective-comments/title-prefix) " "
                                                 (:title objective) " | " (:app-name config/environment))]
                                {:title details
                                 :description details}))}

                (do (log/info (str "Error when retrieving comments " {:comments-http-api-status comments-status}))
                    (default-error-page request 500))))

            (response/redirect (str (utils/path-for :fe/get-comments-for-objective
                                                    :id objective-id)
                                    "?offset=" (max 0 (- comment-count fe-config/comments-pagination))))))
        (error-404-response request)))
    (catch java.lang.NumberFormatException e
      (log/info "Invalid query string: " e)
      (error-404-response request))))

(defn get-comments-for-draft [{:keys [params t'] :as request}]
  (if-let [draft-comments-query (fr/request->draft-comments-query request)]
    (let [objective-id (get-in draft-comments-query [:data :objective-id])
          draft-id (get-in draft-comments-query [:data :draft-id])]
      (case (:status draft-comments-query)
        ::fr/valid
        (let [{objective-status :status objective :result} (http-api/get-objective objective-id)
              {draft-status :status draft :result} (http-api/get-draft objective-id draft-id)
              offset (Integer/parseInt (get-in draft-comments-query [:data :offset]))
              comment-count (get-in draft [:meta :comments-count])]
          (if (every? #(= % ::http-api/success) [objective-status draft-status])
            (if (or (< offset comment-count)
                    (and (= offset 0) (= comment-count 0)))
              (let [comments-query-params {:offset offset
                                           :limit fe-config/comments-pagination}
                    {comments-status :status comments-data :result} (http-api/get-comments
                                                                     (:uri draft)
                                                                     comments-query-params)]
                (if (= comments-status ::http-api/success)
                  {:status 200
                   :headers {"Content-Type" "text/html"}
                   :body (views/draft-comments-view
                           "draft-comments"
                           request
                           :draft draft
                           :comments-data comments-data
                           :objective objective
                           :doc (let [details (str (t' :draft-comments/title-prefix) " "
                                                   (utils/iso-time-string->pretty-time (:_created_at draft)) " | " (:app-name config/environment))]
                                  {:title details
                                   :description details}))}

                  (do (log/info (str "Error when retrieving comments " {:comments-http-api-status comments-status}))
                      (default-error-page request 500))))

              (response/redirect (str (utils/path-for :fe/get-comments-for-draft
                                                      :id objective-id
                                                      :d-id draft-id)
                                      "?offset=" (max 0 (- comment-count fe-config/comments-pagination)))))
            (error-404-response request)))

        ::fr/invalid
        (let [reason (get-in draft-comments-query [:report :offset])]
          (if (some #{:non-int} reason)
            (error-404-response request)
            (response/redirect (str (utils/path-for :fe/get-comments-for-draft
                                                    :id objective-id
                                                    :d-id draft-id)))))))
    (do
      (log/info "Invalid draft query: " (select-keys request [:route-params :params]))
      (error-404-response request))))

(defn post-comment [request]
  (if-let [comment-data (fr/request->comment-data request (get (friend/current-authentication) :identity))]
    (case (:status comment-data)
      ::fr/valid
      (let [{status :status stored-comment :result} (http-api/post-comment (:data comment-data))]
        (cond
          (= status ::http-api/success)
          (-> (redirect-to-params-referer request "comments")
              (assoc :flash {:type :flash-message
                             :message :comment-view/created-message}))

          (= status ::http-api/invalid-input)
          (do (log/info (str "post-comment: Invalid data " {:http-api-status status
                                                                      :data (:data comment-data)}))
              (default-error-page request 400))

          :else
          (do (log/info (str "post-comment: error in api call " {:http-api-status status
                                                     :data (:data comment-data)}))
              (default-error-page request 502))))

      ::fr/invalid
      (-> (redirect-to-params-referer request "add-comment-form")
          (assoc :flash {:validation (dissoc comment-data :status)})))

    (do (log/info (str "post-comment: Invalid data " {:request-params (:params request)}))
        (default-error-page request 400))))

(defn post-annotation [request]
  (if-let [annotation-data (fr/request->annotation-data request (get (friend/current-authentication) :identity))]
    (case (:status annotation-data)
      ::fr/valid
      (let [{status :status stored-comment :result} (http-api/post-comment (:data annotation-data))]
        (cond
          (= status ::http-api/success)
          (-> (redirect-to-params-referer request)
              (assoc :flash {:type :flash-message
                             :message :draft-section/annotation-created-message}))

          (= status ::http-api/invalid-input)
          (do (log/info (str "Invalid data when posting an annotation " {:http-api-status status
                                                                          :data (:data annotation-data)}))
              (default-error-page request 400))

          :else
          (do (log/info (str "Error when posting an annotation " {:http-api-status status
                                                                  :data (:data annotation-data)}))
              (default-error-page request 502))))

      ::fr/invalid
      (-> (redirect-to-params-referer request)
          (assoc :flash {:validation (dissoc annotation-data :status)})))

    (do (log/info (str "Invalid data when posting an annotation " {:request-params (:params request)}))
        (default-error-page request 400))))

;; QUESTIONS

(defn add-a-question [{{id :id} :route-params
                       :keys [uri t' locale] :as request}]
  (let [objective-id (Integer/parseInt id)
        {objective-status :status objective :result} (http-api/get-objective objective-id)]
    (cond
      (every? #(= ::http-api/success %) [objective-status])
      {:status 200
       :body (views/add-question-page "question-create"
                                      request
                                      :objective objective
                                      :doc {:title (str (t' :question-create/doc-title) " to "(:title objective) " | " (:app-name config/environment))
                                            :description (str (t' :question-create/doc-description))})
       :headers {"Content-Type" "text/html"}}

      (= objective-status ::http-api/not-found) (error-404-response request)

      :else
      (do (log/info (str "Error getting objective on create question form " {:http-api-status objective-status}))
          (default-error-page request 500)))))

(defn question-list [{{id :id} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)]
    (response/redirect (utils/path-for :fe/objective :id objective-id))))

(defn add-question-form-post [{:keys [uri t' locale route-params] :as request}]
  (let [question-data (fr/request->question-data request (get (friend/current-authentication) :identity))]
    (case (:status question-data)
      ::fr/valid (let [{status :status question :result} (http-api/create-question (:data question-data))]
                   (cond
                     (= status ::http-api/success)
                     (let [objective-url (str utils/host-url "/objectives/" (:objective-id question) "#questions")]
                       (assoc (response/redirect objective-url) :flash {:type :share-question
                                                                        :created-question question}))

                     (= status ::http-api/invalid-input)
                     (do (log/info (str "add-question-form-post: invalid input when creating question" {:data (:data question-data)}))
                         (default-error-page request 400))

                     :else
                     (do (log/info (str "add-question-form-post: error when creating question" {:http-api-status status}))
                         (default-error-page request 502))))

      ::fr/invalid (-> (response/redirect (utils/path-for :fe/add-a-question
                                                          :id (:id route-params)))
                       (assoc :flash {:validation (dissoc question-data :status)})))))

(defn question-detail [{:keys [route-params uri t' locale params] :as request}]
  (try
    (let [q-id (-> (:q-id route-params) Integer/parseInt)
          o-id (-> (:id route-params) Integer/parseInt)
          offset (Integer/parseInt (get params :offset "0"))
          answer-query (cond-> {}
                         (:offset params) (assoc :offset offset
                                                 :limit fe-config/answers-pagination))
          {question-status :status question :result} (http-api/get-question o-id q-id)
          answers-count (get-in question [:meta :answers-count] 0)]
      (cond
        (= ::http-api/success question-status)
        (if (< offset 0)
          (response/redirect (utils/path-for :fe/question
                                             :q-id q-id
                                             :id o-id))
          (if (or (< offset answers-count)
                  (and (= offset 0) (= answers-count 0)))
            (let [{answer-status :status answers :result} (http-api/retrieve-answers (:uri question) answer-query)
                  {objective-status :status objective :result} (http-api/get-objective (:objective-id question))]
              (if (every? #(= ::http-api/success %) [answer-status objective-status])
                {:status 200
                 :headers {"Content-Type" "text/html"}
                 :body (views/question-page "question-page" request
                                            :objective objective
                                            :question question
                                            :answers answers
                                            :offset offset
                                            :doc {:title (:question question)
                                                  :description (:question question)})}
                (do (log/info (str "question-detail: error retrieving content " {:answer-status answer-status
                                                                                 :objective-status objective-status
                                                                                 :request {:params params
                                                                                           :route-params route-params}}))
                    (default-error-page request 500))))
            (response/redirect (str (utils/path-for :fe/question
                                                    :q-id q-id
                                                    :id o-id)
                                    "?offset=" (max 0 (- answers-count fe-config/answers-pagination))))))

        (= question-status ::http-api/not-found) (error-404-response request)

        (= question-status ::http-api/invalid-input) (do (log/info (str "question-detail: invalid input to get-question"))
                                                         (default-error-page request 400))

        :else
        (do (log/info (str "question-detail: error in question api call "
                           {:question-status question-status
                            :request {:params params :route-params route-params}}))
            (default-error-page request 500))))

    (catch Exception e
      (log/info "question-detail: Exception " e)
      (error-404-response request))))


;; ANSWERS

(defn add-answer-form-post [{:keys [uri t' locale route-params] :as request}]
  (let [answer-data (fr/request->answer-data request (get (friend/current-authentication) :identity))]
    (case (:status answer-data)
      ::fr/valid
      (let [{status :status stored-answer :result} (http-api/create-answer (:data answer-data))]
        (cond
          (= status ::http-api/success)
          (let [answer-url (str utils/host-url "/objectives/" (:objective-id stored-answer)
                                "/questions/" (:question-id stored-answer) "#answer-" (:_id stored-answer))]
            (assoc (response/redirect answer-url) :flash {:type :flash-message
                                                          :message :question-view/added-answer-message}))

          (= status ::http-api/invalid-input)
          (do (log/info (str "add-answer-form-post: create-answer api call invalid input " {:data (:data answer-data)}))
              (default-error-page request 400))

          :else
          (do (log/info (str "add-answer-form-post: create-answer api call error " {:data (:data answer-data)}))
              (default-error-page request 502))))

      ::fr/invalid
      (-> (response/redirect (str (utils/path-for :fe/question
                                                  :id (:id route-params)
                                                  :q-id (:q-id route-params))
                                  "#add-an-answer"))
          (assoc :flash {:validation (dissoc answer-data :status)})))))

;; WRITERS

(defn invite-writer [{{id :id} :route-params
                      :keys [uri t' locale] :as request}]
  (let [objective-id (Integer/parseInt id)
        {status :status objective :result} (http-api/get-objective objective-id)]
    (cond
      (= status ::http-api/success)
      {:status 200
       :body (views/invite-writer-page "invite-writer" request
                                       :objective objective
                                       :doc {:title (str (t' :invite-writer/doc-title) " " (:title objective) " | " (:app-name config/environment))})
       :headers {"Content-Type" "text/html"}}

      (= status ::http-api/not-found)
      (error-404-response request)

      :else
      (do (log/info (str "invite-writer: error in get-objective api call " {:objective-id objective-id
                                                                            :http-api-status status}))
          (default-error-page request 500)))))

(defn writers-list [{{id :id} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)]
    (response/redirect (utils/path-for :fe/objective :id objective-id))))

(defn invitation-form-post [{:keys [t' locale route-params] :as request}]
  (if-let [invitation-data (fr/request->invitation-data request (get (friend/current-authentication) :identity))]
    (case (:status invitation-data)
      ::fr/valid
      (let [{status :status stored-invitation :result} (http-api/create-invitation (:data invitation-data))]
        (case status
          ::http-api/success
          (let [objective-url (str utils/host-url "/objectives/" (:objective-id stored-invitation))
                invitation-url (str utils/host-url "/invitations/" (:uuid stored-invitation))]
            (-> (response/redirect objective-url)
                (assoc :flash {:type :invitation
                               :invitation-url invitation-url
                               :writer-email (:writer-email stored-invitation)})))

          ::http-api/invalid-input
          (do (log/info (str "invitation-form-post: invalid input in create-invitation api call " {:data (:data invitation-data)}))
              (default-error-page request 400))

          (do (log/info (str "invitation-form-post: error in create-invitation api call " {:data (:data invitation-data)
                                                                                           :http-api-status status}))
              (default-error-page request 502))))

      ::fr/invalid
      (-> (response/redirect (utils/path-for :fe/invite-writer :id (:id route-params)))
          (assoc :flash {:validation (dissoc invitation-data :status)})))

    (do (log/info (str "invitation-form-post: fatal data validation error " {:request {:params request
                                                                                       :route-params request}}))
        (default-error-page request 400))))

(defn writer-invitation [{{uuid :uuid} :route-params :keys [t' locale session] :as request}]
  (let [{status :status invitation :result} (http-api/retrieve-invitation-by-uuid uuid)
        {:keys [objective-id _id] invitation-status :status} invitation]
    (cond
      (= status ::http-api/success)
      (cond
        (= invitation-status "active")
        (-> (utils/path-for :fe/objective :id objective-id)
            response/redirect
            (assoc :session session)
            (assoc-in [:session :invitation] {:uuid uuid :objective-id objective-id :invitation-id _id}))

        (= invitation-status "expired")
        (-> (str utils/host-url "/objectives/" objective-id)
            response/redirect
            (assoc :flash {:type :flash-message
                           :message :invitation-response/expired-banner-message}))

        :else (error-404-response request))

      (= status ::http-api/not-found) (error-404-response request)

      :else (do (log/info (str "writer-invitation: error retrieving invitation " {:http-api-status status
                                                                                  :uuid uuid}))
                (default-error-page request 500)))))

(defn create-profile-get [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (views/create-profile "create-profile" request)})

(defn create-profile-post [{:keys [session] :as request}]
  (if (:invitation session)
    (let [profile-data (fr/request->profile-data request (get (friend/current-authentication) :identity))]
      (case (:status profile-data)
        ::fr/valid (let [{status :status} (http-api/post-profile (:data profile-data))]
                     (if (= status ::http-api/success)
                       (accept-invitation request)
                       (do (log/info (str "create-profile-post: error posting profile to api " {:data (:data profile-data)
                                                                                                :http-api-status status}))
                           (default-error-page request 500))))
        ::fr/invalid (-> (response/redirect (utils/path-for :fe/create-profile-get))
                         (assoc :flash {:validation (dissoc profile-data :status)}))))
    (do (log/info (str "create-profile-post: attempt to create a writer profile without an invitation"))
        (default-error-page request 401))))

(defn edit-profile-get [request]
  (if (permissions/writer? (friend/current-authentication))
    (let [{user-status :status user :result} (http-api/get-user (:identity (friend/current-authentication)))]
      (if (= user-status ::http-api/success)
        (let [user-profile (:profile user)]
          {:status 200
           :header {"Content-Type" "text/html"}
           :body (views/edit-profile "edit-profile" request :user-profile user-profile)})

        (do (log/info (str "edit-profile-get: api get-user request error " {:status user-status :user-id (:identity (friend/current-authentication))}))
            (default-error-page request 500))))
    (do (log/info (str "edit-profile-get: attempt to edit profile when not a writer"))
        (default-error-page request 401))))

(defn edit-profile-post [request]
  (if (permissions/writer? (friend/current-authentication))
    (let [profile-data (fr/request->profile-data request (get (friend/current-authentication) :identity))]
      (case (:status profile-data)
        ::fr/valid (let [{status :status} (http-api/post-profile (:data profile-data))]
                     (cond
                       (= status ::http-api/success)
                       (-> (utils/path-for :fe/profile :username (get (friend/current-authentication) :username))
                           response/redirect)

                       :else (do (log/info (str "edit-profile-post: api post-profile error " {:data (:data profile-data)
                                                                                              :http-api-status status}))
                                 (default-error-page request 500))))
        ::fr/invalid (-> (response/redirect (utils/path-for :fe/edit-profile-get))
                         (assoc :flash {:validation (dissoc profile-data :status)}))))
    (do (log/info (str "edit-profile-post: attempt to post profile when not a writer"))
        (default-error-page request 401))))

(defn decline-invitation [{session :session :as request}]
  (if-let [invitation-credentials (:invitation session)]
    (let [{status :status result :result} (http-api/decline-invitation {:invitation-id (:invitation-id invitation-credentials)
                                                                        :objective-id (:objective-id invitation-credentials)
                                                                        :invitation-uuid (:uuid invitation-credentials)})]
      (cond
        (#{::http-api/success ::http-api/invalid-input ::http-api/not-found} status)
        (-> (str utils/host-url)
            response/redirect
            (assoc :flash {:type :flash-message
                           :message :invitation-response/invitation-declined-banner-message})
            (assoc :session session)
            remove-invitation-from-session)

        :else (do (log/info (str "decline-invitation: api error when declining invitation " {:http-api-status status
                                                                                             :invitation-credentials invitation-credentials}))
                  (default-error-page request 500))))
    (do (log/info (str "decline-invitation: attempt to decline invitation without invitation credentials"))
        (default-error-page request 401))))

(defn create-writer [{:keys [session] :as request}]
  (let [invitation-credentials (:invitation session)
        objective-id (:objective-id invitation-credentials)
        writer {:invitee-id (get (friend/current-authentication) :identity)
                :invitation-uuid (:uuid invitation-credentials)
                :objective-id objective-id}
        {status :status} (http-api/post-writer writer)]
    (cond
      (= status ::http-api/success)
      (-> (str utils/host-url "/objectives/" (:objective-id invitation-credentials) "#writers")
          response/redirect
          (assoc :session session)
          remove-invitation-from-session
          (permissions/add-authorisation-role (permissions/writer-inviter-for objective-id))
          (permissions/add-authorisation-role (permissions/writer-for objective-id)))

      :else (do (log/info (str "create-writer: post-writer api error " {:http-api-status status
                                                                        :data writer}))
                (default-error-page request 500)))))

(defn accept-invitation [{:keys [session] :as request}]
  (if-let [invitation-credentials (:invitation session)]
    (if (permissions/writer-for? (friend/current-authentication) (:objective-id invitation-credentials))
      (decline-invitation request)
      (let [{user-status :status user :result} (http-api/get-user (get (friend/current-authentication) :identity))]
        (if-let [profile (:profile user)]
          (create-writer request)
          (-> (utils/path-for :fe/create-profile-get)
              response/redirect
              (assoc :session session)))))
    (do (log/info (str "accept-invitation: attempt to accept invitation without invitation credentials"))
        (default-error-page request 401))))

;;DRAFTS

(defn add-draft-get [{{objective-id :id} :route-params :as request}]
  (let [{objective-status :status objective :result} (http-api/get-objective (Integer/parseInt objective-id))]
    (case objective-status
      ::http-api/success {:status 200
                          :headers {"Content-Type" "text/html"}
                          :body (views/add-draft "add-draft" request :objective-id objective-id)}

      ::http-api/not-found (error-404-response request)

      (do (log/info (str "add-draft-get: api error in get-objective " {:http-api-status objective-status :objective-id objective-id}))
          (default-error-page request 500)))))

(defn add-draft-post [{:keys [params route-params] :as request}]
  (let [draft-data (fr/request->add-draft-data request (get (friend/current-authentication) :identity))]
    (case (:status draft-data)
      ::fr/valid
      (case (:action params)
        "preview"
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body (views/add-draft "add-draft" request
                                :objective-id (-> draft-data :data :objective-id)
                                :preview (utils/hiccup->html (-> draft-data :data :hiccup))
                                :markdown (-> draft-data :data :markdown))}

        "submit"
        (let [draft-info {:submitter-id (-> draft-data :data :submitter-id)
                          :objective-id (-> draft-data :data :objective-id)
                          :content (-> draft-data :data :hiccup)}
              {status :status draft :result} (http-api/post-draft draft-info)]
          (case status
            ::http-api/success (response/redirect (str "/objectives/" (:id route-params) "/drafts/" (:_id draft)))

            ::http-api/not-found (error-404-response request)

            (do (log/info (str "add-draft-post: api error post-draft " {:http-api-status status
                                                                        :data draft-info}))
                (default-error-page request 502)))))

      ::fr/invalid (-> (response/redirect (utils/path-for :fe/add-draft-get :id (:id route-params)))
                       (assoc :flash {:validation (dissoc draft-data :status)})))))

(defn import-draft-get [{{objective-id :id} :route-params :as request}]
  (let [{objective-status :status objective :result} (http-api/get-objective (Integer/parseInt objective-id))]
    (case objective-status
      ::http-api/success {:status 200
                          :headers {"Content-Type" "text/html"}
                          :body (views/import-draft "import-draft" request :objective-id objective-id)}

      ::http-api/not-found (error-404-response request)

      (do (log/info (str "import-draft-get: api error get-objective " {:http-api-status objective-status
                                                                       :objective-id objective-id}))
          (default-error-page request 500)))))

(defn import-draft-post [{:keys [params route-params] :as request}]
  (let [draft-data (fr/request->imported-draft-data request (get (friend/current-authentication) :identity))]
    (case (:status draft-data)
      ::fr/valid (case (:action params)
                   "preview" (-> (response/redirect (utils/path-for :fe/import-draft-get :id  (get-in draft-data [:data :objective-id])))
                                 (assoc :flash {:type :import-draft-preview
                                                :import-draft-preview-html (utils/hiccup->html (get-in draft-data [:data :content]))}))

                   "submit" (let [{status :status draft :result} (http-api/post-draft (:data draft-data))]
                              (cond
                                (= status ::http-api/success)
                                (response/redirect
                                 (utils/path-for :fe/draft
                                                 :id (get-in draft-data [:data :objective-id])
                                                 :d-id (:_id draft)))

                                (= status ::http-api/not-found)
                                (error-404-response request)

                                :else
                                (do (log/info (str "import-draft-post: api error post-draft " {:http-api-status status
                                                                                               :data draft}))
                                    (default-error-page request 502)))))

      ::fr/invalid (-> (response/redirect (utils/path-for :fe/import-draft-get :id (:id route-params)))
                       (assoc :flash {:validation (dissoc draft-data :status)})))))

(defn draft [request]
  (if-let [draft-query-data (fr/request->draft-query request)]
    (let [objective-id (get-in draft-query-data [:data :objective-id])
          draft-id (get-in draft-query-data [:data :draft-id])
          {objective-status :status objective :result} (http-api/get-objective objective-id)
          {draft-status :status draft :result} (http-api/get-draft objective-id draft-id)
          {comments-status :status comments-data :result} (http-api/get-comments (:uri draft) {:limit fe-config/comments-pagination})
          {writers-status :status writers :result} (http-api/retrieve-writers objective-id)]
      (cond
        (every? #(= ::http-api/success %) [objective-status draft-status writers-status comments-status])
        (let [draft-content (utils/hiccup->html (:content draft))
              {sections :result} (http-api/get-draft-sections (:uri draft))]
          {:status 200
           :body (views/draft "draft" request
                              :objective objective
                              :writers writers
                              :comments-data comments-data
                              :draft-content draft-content
                              :draft draft
                              :sections sections)
           :headers {"Content-Type" "text/html"}})

        (= objective-status ::http-api/not-found)
        (error-404-response request)

        (or (= draft-status ::http-api/forbidden) (= draft-status ::http-api/not-found))
        (if (= draft-id "latest")
          {:status 200
           :body (views/draft "draft" request
                              :writers writers
                              :objective objective)
           :headers {"Content-Type" "text/html"}}
          (error-404-response request))

        :else (do (log/info (str "draft: error in api calls "
                                 {:draft-http-api-status draft-status
                                  :objective-http-api-status objective-status
                                  :comments-http-api-status comments-status
                                  }))
                  (default-error-page request 500))))

    (do (log/info "draft: Invalid draft query" (select-keys request [:route-params :params]))
        (default-error-page request 400))))

(defn draft-diff [request]
  (if-let [draft-query (fr/request->draft-query request)]
    (let [objective-id (get-in draft-query [:data :objective-id ])
          draft-id (get-in draft-query [:data :draft-id])
          {objective-status :status objective :result} (http-api/get-objective objective-id)
          {draft-status :status draft :result} (http-api/get-draft objective-id draft-id)]
      (if-let [previous-draft-id (:previous-draft-id draft)]
        (let [{previous-draft-status :status previous-draft :result} (http-api/get-draft objective-id (:previous-draft-id draft))]
          (if
            (every? #(= ::http-api/success %) [objective-status draft-status previous-draft-status])
            (let [diffs (diffs/get-diffs-between-drafts draft previous-draft)]
              {:status 200
               :body (views/draft-diff "draft-diff" request
                                       :current-draft draft
                                       :previous-draft-diffs (-> diffs
                                                                 :previous-draft-diffs
                                                                 utils/hiccup->html)
                                       :current-draft-diffs (-> diffs
                                                                :current-draft-diffs
                                                                utils/hiccup->html))
               :headers {"Content-Type" "text/html"}})))
        (error-404-response request)))
    (do (log/info "draft: invalid draft query " (select-keys request [:route-params]))
        (default-error-page request 400))))

(defn draft-list [{{:keys [id]} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)
        {objective-status :status objective :result} (http-api/get-objective objective-id)
        drafts (:result (http-api/get-all-drafts objective-id))
        writers (:result (http-api/retrieve-writers objective-id))]
    (case objective-status
      ::http-api/success {:status 200
                          :body (views/draft-list "draft-list" request
                                                  :objective objective
                                                  :writers writers
                                                  :drafts drafts)
                          :headers {"Content-Type" "text/html"}}

      ::http-api/not-found (error-404-response request)

      (do (log/info (str "draft-list: api error get-objective " {:http-api-status objective-status
                                                                 :objective-id objective-id}))
          (default-error-page request 500)))))

(defn draft-section [{:keys [uri] :as request}]
  (let [{section-status :status section :result} (http-api/get-draft-section uri)
        {comments-data :result} (http-api/get-comments uri {:limit fe-config/comments-pagination})]
    (cond
      (= ::http-api/success section-status)
      {:status 200
       :body (views/draft-section "draft-section" request
                                  :section (update-in section [:section] utils/hiccup->html)
                                  :comments-data comments-data)
       :headers {"Content-Type" "text/html"}}

      (= ::http-api/not-found)
      (error-404-response request)

      :else (do (log/info (str "draft-section: api error get-draft-section "
                               {:http-api-status section-status
                                :uri uri}))
                (default-error-page request 500)))))

(defn post-up-vote [request]
  (-> (fr/request->up-vote-info request (get (friend/current-authentication) :identity))
      http-api/create-up-down-vote)
  (redirect-to-params-referer request))

(defn post-down-vote [request]
  (-> (fr/request->down-vote-info request (get (friend/current-authentication) :identity))
      http-api/create-up-down-vote)
  (redirect-to-params-referer request))

(defn post-star [{:keys [t'] :as request}]
  (let [star-data (fr/request->star-info request (get (friend/current-authentication) :identity))
        {status :status stored-star :result} (http-api/post-star star-data)]
    (cond
      (= status ::http-api/success)
      (redirect-to-params-referer request)

      (= status ::http-api/invalid-input)
      (do (log/info (str "post-star: invalid input when posting star "
                         {:http-api-status status
                          :data star-data}))
          (default-error-page request 400))

      :else
      (do (log/info (str "post-star: api error post-star "
                         {:http-api-status status
                          :data star-data}))
          (default-error-page request 502)))))

(defn post-mark [request]
  (if-let [mark-data (fr/request->mark-info request (get (friend/current-authentication) :identity))]
    (let [{status :status mark :result} (http-api/post-mark mark-data)]
      (case status
        ::http-api/success
        (redirect-to-params-referer request)

        ::http-api/invalid-input
        (do (log/info (str "post-mark: invalid input to api for post-mark "
                           {:data mark-data}))
            (default-error-page request 400))

        (do (log/info (str "post-mark: api error post-mark "
                           {:http-api-status status
                            :data mark-data}))
            (default-error-page request 502))))

    (do (log/info (str "post-mark: fatal validation error "
                       {:request {:params (:params request)
                                  :route-params (:route-params request)}
                        :authenticated-user-id (get (friend/current-authentication) :identity)}))
        (default-error-page request 400))))

;;ADMINS

(defn post-admin-removal-confirmation [request]
  (if-let [admin-removal-confirmation-data (fr/request->admin-removal-confirmation-info request (get (friend/current-authentication) :identity))]
    (let [{status :status admin-removal :result} (http-api/post-admin-removal admin-removal-confirmation-data)
          updated-session (dissoc (:session request) :removal-data)]
      (case status
        ::http-api/success
        (-> (response/redirect (utils/path-for :fe/objective-list))
            (assoc :session updated-session))

        ::http-api/invalid-input
        (do (log/info (str "post-admin-removal-confirmation: invalid input to post-admin-removal api "
                           {:data admin-removal-confirmation-data}))
            (default-error-page request 400))

        (do (log/info (str "post-admin-removal-confirmation: api error post-admin-removal "
                           {:http-api-status status
                            :data admin-removal-confirmation-data}))
            (default-error-page request 502))))

    (do (log/info (str "post-admin-removal-confirmation: fatal validation error "
                       {:request {:params (:params request)
                                  :route-params (:route-params request)}
                        :authenticated-user-id (get (friend/current-authentication) :identity)}))
        (default-error-page request 400))))

(defn admin-removal-confirmation [request]
  (if-let [removal-data (fr/request->removal-data request)]
    {:status 200
     :body (views/admin-removal-confirmation "admin-removal-confirmation" request
                                             :removal-data removal-data)
     :headers {"Content-Type" "text/html"}}
    (error-404-response request)))

(defn post-admin-removal [request]
  (if-let [admin-removal-data (fr/request->admin-removal-info request)]
    (let [updated-session (assoc (:session request) :removal-data admin-removal-data)]
      (-> (response/redirect (utils/path-for :fe/admin-removal-confirmation-get))
          (assoc :session updated-session)))

    (do (log/info (str "post-admin-removal: fatal validation error "
                       {:request {:params (:params request)
                                  :route-params (:route-params request)}}))
        (default-error-page request 400))))

(defn admin-activity [request]
  (let [{status :status admin-removals :result} (http-api/get-admin-removals)]
    (cond
      (= status ::http-api/success)
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (views/admin-activity "admin-activity" request
                                      :admin-removals admin-removals)}

      :else
      (do (log/info (str "admin-activity: api error get-admin-removals "
                         {:http-api-status status}))
          (default-error-page request 502)))))

    ;; Promoting objectives

(defn post-promote-objective [request]
  (if-let [promoted-data (fr/request->promoted-data request (get (friend/current-authentication) :identity))]
    (let [{status :status promoted-objective :result} (http-api/post-promote-objective promoted-data)
          updated-session (dissoc (:session request) :removal-data)]
      (case status
        ::http-api/success
        (-> (response/redirect (utils/path-for :fe/objective-list))
            (assoc :session updated-session))

        ::http-api/invalid-input
        (do (log/info (str "post--promote-objective: invalid input to post-promote-objective api "
                           {:data [promoted-data]}))
            (default-error-page request 400))

        (do (log/info (str "post-promote-objective: api error post-promote-objective "
                           {:http-api-status status
                            :data promoted-data}))
            (default-error-page request 502))))


    (do (log/info (str "post-promote-objective: fatal validation error "
                           {:request {:params (:params request)
                                      :route-params (:route-params request)}
                            :authenticated-user-id (get (friend/current-authentication) :identity)}))
            (default-error-page request 400))))
