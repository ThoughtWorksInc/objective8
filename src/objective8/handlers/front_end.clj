(ns objective8.handlers.front-end
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [org.httpkit.client :as http]
            [objective8.http-api :as http-api]
            [objective8.front-end-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.permissions :as permissions]
            [objective8.views :as views]))

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

(defn index [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (views/index "index" request)})

(defn sign-in [{{refer :refer} :params :as request}]
  {:status 200
   :header {"Content-Type" "text/html"}  
   :body (views/sign-in "sign-in" request)
   :session (assoc (:session request) :sign-in-referrer refer)})

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

;; USER PROFILE

(defn sign-up-form [{:keys [t' locale errors] :as request}]
  {:status 200
   :header {"Content-Type" "text/html"}  
   :body (views/sign-up "sign-up" request :errors errors)})

(defn profile [{:keys [route-params] :as request}]
  (let [username (:username route-params)
        {user-status :status user :result} (http-api/find-user-by-username username)
        user-profile (:profile user)
        joined-date (utils/iso-time-string->pretty-date (:_created_at user))]
    (cond
      (= user-status ::http-api/success) {:status 200
                                          :header {"Content-Type" "text/html"}  
                                          :body (views/profile "profile" request 
                                                               :user-profile user-profile
                                                               :joined-date joined-date
                                                               :doc {:title (str (:name user-profile) " | Objective[8]")})}
      (= user-status ::http-api/not-found) (error-404-response request)
      :else {:status 500})))


;; OBJECTIVES
(defn format-objective [objective]
  (let [goals (if (:goals objective)
                (list (:goals objective))
                (remove clojure.string/blank? [(:goal-1 objective) (:goal-2 objective) (:goal-3 objective)]))
        formatted-objective (-> objective
                                (assoc :goals goals)
                                (assoc :days-until-drafting-begins (utils/days-until (:end-date objective)))
                                (update-in [:end-date] utils/date-time->pretty-date)
                                (dissoc :goal-1 :goal-2 :goal-3))]
    formatted-objective))

(defn objective-list [request]
  (let [signed-in-id (get (friend/current-authentication) :identity)
        {status :status objectives :result} (if signed-in-id
                                              (http-api/get-objectives {:signed-in-id signed-in-id})
                                              (http-api/get-objectives))]
    (cond 
      (= status ::http-api/success)
      {:status 200
       :header {"Content-Type" "text/html"}  
       :body (views/objective-list "objective-list" request
                                   :objectives (map format-objective objectives))} 
      
      (= status ::http-api/error)
      {:status 502})))

(defn create-objective-form [request]
  {:status 200
   :header {"Content-Type" "text/html"}  
   :body (views/create-objective "create-objective" request)})

(defn create-objective-form-post [{:keys [t' locale session] :as request}]
  (if-let [objective (helpers/request->objective request (get (friend/current-authentication) :identity))]
    (let [{status :status stored-objective :result} (http-api/create-objective objective)]
      (cond (= status ::http-api/success)
            (let [objective-url (str utils/host-url "/objectives/" (:_id stored-objective))]
              (-> (response/redirect objective-url)
                  (assoc :flash {:type :flash-message
                                 :message :objective-view/created-message}
                         :session session)
                  (permissions/add-authorisation-role (permissions/writer-for (:_id stored-objective)))
                  (permissions/add-authorisation-role (permissions/writer-inviter-for (:_id stored-objective)))))

            (= status ::http-api/invalid-input) {:status 400}

            :else {:status 502}))
    {:status 400}))

(defn remove-invitation-from-session [response]
  (update-in response [:session] dissoc :invitation))

(defn update-session-invitation [{:keys [session] :as request}]
  (if-let [invitation-rsvp (:invitation session)]
    (let [invitation-status (:status (http-api/retrieve-invitation-by-uuid (:uuid invitation-rsvp)))]
      (if (= ::http-api/success invitation-status)
        request
        (remove-invitation-from-session request)))
    request))

(defn objective-detail [{{:keys [id]} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)
        updated-request (update-session-invitation request)
        signed-in-id (get (friend/current-authentication) :identity)
        {objective-status :status objective :result} (if signed-in-id
                                                       (http-api/get-objective objective-id {:signed-in-id signed-in-id})
                                                       (http-api/get-objective objective-id))
        {candidate-status :status candidates :result} (http-api/retrieve-candidates objective-id)
        {questions-status :status questions :result} (http-api/retrieve-questions objective-id)
        {comments-status :status comments :result} (http-api/get-comments (:uri objective))]
    (cond
      (every? #(= ::http-api/success %) [objective-status candidate-status questions-status comments-status])
      (let [formatted-objective (format-objective objective)]
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body
        (views/objective-detail-page "objective-details"
                                     updated-request
                                     :objective formatted-objective
                                     :candidates candidates
                                     :questions questions
                                     :comments comments
                                     :doc (let [details (str (:title objective) " | Objective[8]")]
                                            {:title details
                                             :description details}))})
      (= objective-status ::http-api/not-found) (error-404-response updated-request)

      (= objective-status ::http-api/invalid-input) {:status 400}

      :else {:status 500})))

;; COMMENTS

(defn post-comment [{:keys [t'] :as request}]
  (if-let [comment-data (helpers/request->comment-data request (get (friend/current-authentication) :identity))]
    (let [{status :status stored-comment :result} (http-api/post-comment comment-data)]
      (cond
        (= status ::http-api/success)
        (-> (redirect-to-params-referer request "comments")
            (assoc :flash {:type :flash-message
                           :message :comment-view/created-message}))

        (= status ::http-api/invalid-input) {:status 400}

        :else {:status 502}))
    {:status 400}))

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
                   :objective (format-objective objective)
                   :doc {:title (str (t' :question-create/doc-title) " to "(:title objective) " | Objective[8]")
                         :description (str (t' :question-create/doc-description))}
                   )
           :headers {"Content-Type" "text/html"}}

          (= objective-status ::http-api/not-found) (error-404-response request)
          :else {:status 500})))

(defn question-list [{{id :id} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)]
    (response/redirect (utils/path-for :fe/objective :id objective-id))))

(defn add-question-form-post [{:keys [uri t' locale] :as request}]
  (if-let [question-data (helpers/request->question request (get (friend/current-authentication) :identity))]
    (let [{status :status question :result} (http-api/create-question question-data)]
      (cond 
        (= status ::http-api/success)
        (let [objective-url (str utils/host-url "/objectives/" (:objective-id question) "#questions")]
          (assoc (response/redirect objective-url) :flash {:type :share-question
                                                           :created-question question}))

        (= status ::http-api/invalid-input) {:status 400}

        :else {:status 502}))
    {:status 400}))

(defn question-detail [{:keys [route-params uri t' locale] :as request}]
  (let [q-id (-> (:q-id route-params) Integer/parseInt)
        o-id (-> (:id route-params) Integer/parseInt)
        {question-status :status question :result} (http-api/get-question o-id q-id)]
    (cond
      (= ::http-api/success question-status)
      (let [{answer-status :status answers :result} (http-api/retrieve-answers (:objective-id question) (:_id question))
          {objective-status :status objective :result} (http-api/get-objective (:objective-id question))]
      (if (every? #(= ::http-api/success %) [answer-status objective-status])
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body (views/question-page "question-page" request
                                    :objective (format-objective objective)
                                    :question question
                                    :answers answers
                                    :doc {:title (:question question)
                                          :description (:question question)})}
        {:status 500}))

      (= question-status ::http-api/not-found) (error-404-response request)

      (= question-status ::http-api/invalid-input) {:status 400}

      :else {:status 500})))


;; ANSWERS

(defn add-answer-form-post [{:keys [uri t' locale] :as request}]
  (if-let [answer (helpers/request->answer-info request (get (friend/current-authentication) :identity))]
    (let [{status :status stored-answer :result} (http-api/create-answer answer)]
      (cond
        (= status ::http-api/success)
        (let [answer-url (str utils/host-url "/objectives/" (:objective-id stored-answer)
                              "/questions/" (:question-id stored-answer))]
          (assoc (response/redirect answer-url) :flash {:type :flash-message
                                                        :message :question-view/added-answer-message}))

        (= status ::http-api/invalid-input) {:status 400}

        :else {:status 502}))
    {:status 400}))

;; WRITERS

(defn invite-writer [{{id :id} :route-params
                      :keys [uri t' locale] :as request}]
  (let [objective-id (Integer/parseInt id)
        {status :status objective :result} (http-api/get-objective objective-id)]
    (cond
      (= status ::http-api/success)

      {:status 200
       :body (views/invite-writer-page "invite-writer" request
               :objective (format-objective objective)
               :doc {:title (str (t' :invite-writer/doc-title) " " (:title objective) " | Objective[8]")})
       :headers {"Content-Type" "text/html"}}

      (= status ::http-api/not-found) (error-404-response request)
      :else {:status 500})))

(defn candidate-list [{{id :id} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)]
    (response/redirect (utils/path-for :fe/objective :id objective-id))))

(defn invitation-form-post [{:keys [t' locale] :as request}]
  (if-let [invitation (helpers/request->invitation-info request (get (friend/current-authentication) :identity))]
    (let [{status :status stored-invitation :result} (http-api/create-invitation invitation)]

      (cond
        (= status ::http-api/success)
        (let [objective-url (str utils/host-url "/objectives/" (:objective-id stored-invitation))
              invitation-url (str utils/host-url "/invitations/" (:uuid stored-invitation))]
          (assoc (response/redirect objective-url) :flash {:type :invitation
                                                           :invitation-url invitation-url
                                                           :writer-email (:writer-email stored-invitation)}))
        (= status ::http-api/invalid-input) {:status 400}
        :else {:status 502}))
    {:status 400}))

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
      :else {:status 500})))

(defn create-profile-get [request]
  {:status 200
   :headers {"Content-Type" "text/html"}      
   :body (views/create-profile "create-profile" request)})

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
       
       :else {:status 500}))
    {:status 401}))

(defn create-writer [{:keys [session] :as request}]
  (let [invitation-credentials (:invitation session)
        objective-id (:objective-id invitation-credentials) 
        candidate-writer {:invitee-id (get (friend/current-authentication) :identity)
                          :invitation-uuid (:uuid invitation-credentials)
                          :objective-id objective-id}
        {status :status} (http-api/post-candidate-writer candidate-writer) ]
    (cond
      (= status ::http-api/success)
      (-> (str utils/host-url "/objectives/" (:objective-id invitation-credentials) "#writers")
          response/redirect
          (assoc :session session)
          remove-invitation-from-session
          (permissions/add-authorisation-role (permissions/writer-inviter-for objective-id))
          (permissions/add-authorisation-role (permissions/writer-for objective-id)))

      :else {:status 500})))

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
    {:status 401}))

(defn create-profile-post [{:keys [session] :as request}]
  (if (:invitation session) 
    (let [profile-data (helpers/request->profile-info request (get (friend/current-authentication) :identity))
          {status :status} (http-api/post-profile profile-data)]
      (cond
        (= status ::http-api/success)
        (accept-invitation request) 
        :else {:status 500}))
    {:status 401}))

;;DRAFTS

(defn add-draft-get [{{objective-id :id} :route-params :as request}]
   (let [{objective-status :status objective :result} (http-api/get-objective (Integer/parseInt objective-id))]
     (cond
       (= objective-status ::http-api/success)
       (if (= "drafting" (:status objective))
         {:status 200
          :headers {"Content-Type" "text/html"}      
          :body (views/add-draft "add-draft" request :objective-id objective-id)} 
         {:status 401}) 
       (= objective-status ::http-api/not-found) (error-404-response request)
       :else {:status 500}))) 

(defn import-draft-get [{{objective-id :id} :route-params :as request}]
   (let [{objective-status :status objective :result} (http-api/get-objective (Integer/parseInt objective-id))]
     (cond
       (= objective-status ::http-api/success)
       (if (= "drafting" (:status objective))
         {:status 200
          :headers {"Content-Type" "text/html"}      
          :body (views/import-draft "import-draft" request :objective-id objective-id)} 
         {:status 401}) 
       (= objective-status ::http-api/not-found) (error-404-response request)
       :else {:status 500})))

(defn add-draft-post [{{o-id :id} :route-params
                        {content :content action :action} :params
                        :as request}]
  (let [parsed-markdown (utils/markdown->hiccup content)
        objective-id (Integer/parseInt o-id)]
    (cond
      (= action "preview")
      (let [preview (utils/hiccup->html parsed-markdown)]
        {:status 200
         :headers {"Content-Type" "text/html"}      
         :body (views/add-draft "add-draft" request :objective-id objective-id 
                                :preview preview :markdown content)})

      (= action "submit")
      (let [{status :status draft :result} (http-api/post-draft {:objective-id objective-id
                                                                 :submitter-id (get (friend/current-authentication) 
                                                                                    :identity)
                                                                 :content parsed-markdown})]
        (cond
          (= status ::http-api/success) (response/redirect (str "/objectives/" o-id "/drafts/" (:_id draft)))    
          (= status ::http-api/not-found) {:status 404}
          :else {:status 502})))))

(defn import-draft-post [{:keys [params] :as request}]
  (let [action (:action params)
        draft-data (helpers/request->draft-info request (get (friend/current-authentication) :identity))]
    (cond
      (= action "preview")
      (-> (response/redirect (utils/path-for :fe/import-draft-get :id  (:objective-id draft-data) ))
          (assoc :flash {:type :import-draft-preview
                         :import-draft-preview-html (utils/hiccup->html (:content draft-data))}))
      (= action "submit")
      (let [{status :status draft :result} (http-api/post-draft draft-data)]
        (cond
          (= status ::http-api/success) 
          (response/redirect (utils/path-for :fe/draft :id (:objective-id draft-data) :d-id (:_id draft)))
          (= status ::http-api/not-found) {:status 404}
          :else {:status 502})))))

(defn draft [{{:keys [d-id id]} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)
        draft-id (if (= d-id "latest")
                   d-id
                   (Integer/parseInt d-id))
        {objective-status :status objective :result} (http-api/get-objective objective-id)
        {draft-status :status draft :result} (http-api/get-draft objective-id draft-id)
        {comments-status :status comments :result} (http-api/get-comments (:uri draft))
        {candidate-status :status candidates :result} (http-api/retrieve-candidates objective-id)]
    (cond
      (every? #(= ::http-api/success %) [objective-status draft-status candidate-status comments-status])
      (let [draft-content (utils/hiccup->html (apply list (:content draft)))]
        {:status 200
         :body (views/draft "draft" request
                            :objective objective
                            :candidates candidates
                            :comments comments
                            :draft-content draft-content
                            :draft draft)
         :headers {"Content-Type" "text/html"}})

      (= objective-status ::http-api/not-found)
      (error-404-response request)

      (or (= draft-status ::http-api/forbidden) (= draft-status ::http-api/not-found))
      (if (= d-id "latest")
        {:status 200
         :body (views/draft "draft" request
                            :candidates candidates
                            :objective (format-objective objective))
         :headers {"Content-Type" "text/html"}}
        (error-404-response request))

      :else {:status 500})))

(defn draft-list [{{:keys [id]} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)
        {objective-status :status objective :result} (http-api/get-objective objective-id)
        {drafts-status :status drafts :result} (http-api/get-all-drafts objective-id)
        {candidate-status :status candidates :result} (http-api/retrieve-candidates objective-id)]
    (cond
      (every? #(= ::http-api/success %) [drafts-status objective-status candidate-status])
      {:status 200
       :body (views/draft-list "draft-list" request
                               :objective (format-objective objective)
                               :candidates candidates
                               :drafts drafts)
       :headers {"Content-Type" "text/html"}} 

      (= drafts-status ::http-api/forbidden)
      {:status 200
       :body (views/draft-list "draft-list" request
                               :objective (format-objective objective))
       :headers {"Content-Type" "text/html"}}

      (= objective-status ::http-api/not-found)
      (error-404-response request)
      :else {:status 500})))

(defn post-up-vote [request]
  (-> (helpers/request->up-vote-info request (get (friend/current-authentication) :identity))
      http-api/create-up-down-vote)
  (redirect-to-params-referer request))

(defn post-down-vote [request]
  (-> (helpers/request->down-vote-info request (get (friend/current-authentication) :identity))
      http-api/create-up-down-vote)
  (redirect-to-params-referer request))

(defn post-star [{:keys [t'] :as request}]
  (let [star-data (helpers/request->star-info request (get (friend/current-authentication) :identity))
        {status :status stored-star :result} (http-api/post-star star-data)]
    (cond
      (= status ::http-api/success) (redirect-to-params-referer request)
      
      (= status ::http-api/invalid-input) {:status 400}

      :else {:status 502}))) 

(defn post-mark [request]
  (if-let [mark-data (helpers/request->mark-info request (get (friend/current-authentication) :identity))]
    (let [{status :status mark :result} (http-api/post-mark mark-data)]
      (case status
        ::http-api/success (redirect-to-params-referer request)
        ::http-api/invalid-input {:status 400}

        {:status 502}))
    {:status 400}))
