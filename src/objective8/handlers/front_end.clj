(ns objective8.handlers.front-end
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [org.httpkit.client :as http]
            [objective8.responses :refer :all]
            [objective8.http-api :as http-api]
            [objective8.front-end-helpers :as helpers]
            [objective8.utils :as utils]
            [objective8.views :as views]))

;; HELPERS

(defn signed-in? []
  (friend/authorized? #{:signed-in} friend/*identity*))

(defn error-404-response [request]
  (assoc (views/four-o-four "error-404" request) :status 404))

(defn invitation? [session]
  (:invitation session))

;; HANDLERS

(defn error-404 [request]
  (error-404-response request))

(defn index [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (views/index "index" request)})

(defn sign-in [{{refer :refer} :params
                :keys [t' locale]
                :as request}]
  (-> (views/sign-in "sign-in" request)
      (assoc :session (:session request))
      (assoc-in [:session :sign-in-referrer] refer)))

(defn sign-out [request]
  (assoc
    (friend/logout* (response/redirect "/"))
    :session {}))

(defn project-status [{:keys [t' locale] :as request}]
  (views/project-status "project-status" request))

(defn learn-more [request]
  {:status 200
   :header {"Content-Type" "text/html"}  
   :body (views/learn-more-page "learn-more" request)})

;; USER PROFILE

(defn sign-up-form [{:keys [t' locale errors] :as request}]
  (views/sign-up-form "sign-up" request :errors errors))

;; OBJECTIVES
(defn format-objective [objective]
  (let [goals (if (:goals objective)
                (list (:goals objective))
                (remove clojure.string/blank? [(:goal-1 objective) (:goal-2 objective) (:goal-3 objective)]))
        formatted-objective (-> objective
                                (assoc :goals goals)
                                (assoc :drafting-begins-in (str (utils/days-until (:end-date objective)) " days"))
                                (update-in [:end-date] utils/date-time->pretty-date)
                                (dissoc :goal-1 :goal-2 :goal-3))]
    formatted-objective))

(defn objective-list [request]
  (let [{status :status objectives :result} (http-api/get-all-objectives)]
    (cond 
      (= status ::http-api/success)
      {:status 200
       :header {"Content-Type" "text/html"}  
       :body (views/objective-list "objective-list" request
                                   :objectives (map format-objective objectives))} 
      
      (= status ::http-api/error)
      {:status 502})))

(defn create-objective-form [{:keys [t' locale] :as request}]
  (views/create-objective-form "objective-create" request))

(defn create-objective-form-post [{:keys [t' locale] :as request}]
  (if-let [objective (helpers/request->objective request (get (friend/current-authentication) :identity))]
    (let [{status :status stored-objective :result} (http-api/create-objective objective)]
      (cond (= status ::http-api/success)
            (let [objective-url (str utils/host-url "/objectives/" (:_id stored-objective))
                  message (t' :objective-view/created-message)]
              (assoc (response/redirect objective-url) :flash message)) 

            (= status ::http-api/invalid-input) {:status 400}

            :else {:status 502}))
    {:status 400}))

(defn objective-detail [{{:keys [id]} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)
        {objective-status :status objective :result} (http-api/get-objective objective-id)
        {candidate-status :status  candidates :result} (http-api/retrieve-candidates objective-id)  
        {questions-status :status questions :result} (http-api/retrieve-questions objective-id) 
        {comments-status :status comments :result} (http-api/retrieve-comments objective-id)]
    (cond
      (every? #(= ::http-api/success %) [objective-status candidate-status questions-status comments-status])
      (let [formatted-objective (format-objective objective)]
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body
        (views/objective-detail-page "objective-details"
                                     request
                                     :objective formatted-objective
                                     :candidates candidates
                                     :questions questions
                                     :comments comments
                                     :doc (let [details (str (:title objective) " | Objective[8]")]
                                            {:title details
                                             :description details}))})
      (= objective-status ::http-api/not-found) (error-404-response request)

      (= objective-status ::http-api/invalid-input) {:status 400}

      :else {:status 500})))

;; COMMENTS

(defn create-comment-form-post [{{objective-id :objective-id :as params} :params
                                 :keys [t' locale] :as request}]
  (if-let [comment-data (helpers/request->comment-data request (get (friend/current-authentication) :identity))]
    (let [{status :status stored-comment :result} (http-api/post-comment comment-data)]
      (cond
        (= status ::http-api/success)
        (let [objective-url (str utils/host-url "/objectives/" objective-id "#comments")
              message (t' :comment-view/created-message)]
          (assoc (response/redirect objective-url) :flash message))

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
          :else {:status 500})
        ))

(defn question-list [{{id :id} :route-params
                      :keys [uri t' locale] :as request}]
  (let [objective-id (Integer/parseInt id)
        {objective-status :status objective :result} (http-api/get-objective objective-id)
        {questions-status :status questions :result} (http-api/retrieve-questions objective-id)]
    (cond
      (every? #(= ::http-api/success %) [objective-status questions-status])
      (views/question-list "question-list"
                           request
                           :objective (format-objective objective)
                           :questions questions
                           :doc {:title (str (:title objective) " | Objective[8]")
                                 :description (str (t' :question-list/questions-about) " " (:title objective))})

      (= objective-status ::http-api/not-found) (error-404-response request)
      (= questions-status ::http-api/not-found) (error-404-response request)
      (= questions-status ::http-api/invalid-input) {:status 400}
      :else {:status 500})))

(defn add-question-form-post [{:keys [uri t' locale] :as request}]
  (if-let [question (helpers/request->question request (get (friend/current-authentication) :identity))]
    (let [{status :status stored-question :result} (http-api/create-question question)]
      (cond 
        (= status ::http-api/success)
        (let [objective-url (str utils/host-url "/objectives/" (:objective-id stored-question) "#questions")
              message (t' :question-view/added-message)]
          (assoc (response/redirect objective-url) :flash message))

        (= status ::http-api/invalid-input) {:status 400}

        :else {:status 502}))
    {:status 400}))

(defn question-detail [{{q-id :q-id id :id} :route-params
                        message :flash
                        :keys [uri t' locale]
                        :as request}]
  (let [{question-status :status question :result} (http-api/get-question (Integer/parseInt id) (Integer/parseInt q-id))
        {answer-status :status answers :result} (http-api/retrieve-answers (:objective-id question) (:_id question))
        {objective-status :status objective :result} (http-api/get-objective (:objective-id question))]
    (cond
      (every? #(= ::http-api/success %) [question-status answer-status objective-status])
      {:status 200
       :headers {"Content-Type" "text/html"}      
       :body (views/question-page "answer-create" request
                                  :objective (format-objective objective)
                                  :question question
                                  :answers answers)}
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
                              "/questions/" (:question-id stored-answer))
              message (t' :question-view/added-answer-message)]
          (assoc (response/redirect answer-url) :flash message))

        (= status ::http-api/invalid-input) {:status 400}

        :else {:status 502}))
    {:status 400}))

;; WRITERS

(defn invite-writer [{{id :id} :route-params
                      :keys [uri t' locale] :as request}]
  (let [objective-id (Integer/parseInt id)
        {objective-status :status objective :result} (http-api/get-objective objective-id)]
    (cond
      (every? #(= ::http-api/success %) [objective-status])

      {:status 200
       :body (views/invite-writer-page "invite-writer" request
               :objective (format-objective objective)
               :doc {:title (str (t' :invite-writer/doc-description) " " (:title objective) " | Objective[8]")
                     :description (str (t' :invite-writer/doc-description) " " (:title objective))})
       :headers {"Content-Type" "text/html"}}

      (= objective-status ::http-api/not-found) (error-404-response request)
      :else {:status 500})))

(defn candidate-list [{{id :id} :route-params
                       :keys [uri t' locale] :as request}]
  (let [objective-id (Integer/parseInt id)
        {objective-status :status objective :result} (http-api/get-objective objective-id)
        {candidate-status :status  candidates :result} (http-api/retrieve-candidates objective-id)]
    (cond
      (every? #(= ::http-api/success %) [candidate-status objective-status])
      (views/candidate-list "candidate-list"
                            request
                            :objective (format-objective objective)
                            :candidates candidates)
      #_(rendered-response candidate-list-page {:translation t'
                                                :locale (subs (str locale) 1)
                                                :doc-title (t' :invitation/doc-title)
                                                :doc-description (t' :invitation/doc-description)
                                                :objective (format-objective objective)
                                                :candidates candidates
                                                :uri uri
                                                :invitation (invitation? (:session request))
                                                :signed-in (signed-in?)})
      (= objective-status ::http-api/not-found) (error-404-response request)
      (= candidate-status ::http-api/not-found) (error-404-response request)
      (= candidate-status ::http-api/invalid-input) {:status 400}
      :else {:status 500})))

(defn invitation-form-post [{:keys [t' locale] :as request}]
  (if-let [invitation (helpers/request->invitation-info request (get (friend/current-authentication) :identity))]
    (let [{status :status stored-invitation :result} (http-api/create-invitation invitation)]
      (cond
        (= status ::http-api/success)
        (let [objective-url (str utils/host-url "/objectives/" (:objective-id stored-invitation))
              invitation-url (str utils/host-url "/invitations/" (:uuid stored-invitation))
              message (str "Your invited writer can accept their invitation by going to " invitation-url)]
          (assoc (response/redirect objective-url) :flash message))
        (= status ::http-api/invalid-input) {:status 400}
        :else {:status 502}))
    {:status 400}))

(defn writer-invitation [{{uuid :uuid} :route-params :keys [t' locale session] :as request}]
  (let [{status :status
         invitation :result} (http-api/retrieve-invitation-by-uuid uuid)
         {:keys [objective-id _id] invitation-status :status} invitation]


    (cond

      (= status ::http-api/success)
      (cond
        (= invitation-status "active")
        (-> (str utils/host-url "/objectives/" objective-id "/writer-invitations/" _id)
            response/redirect
            (assoc :session session)
            (assoc-in [:session :invitation] {:uuid uuid :objective-id objective-id :invitation-id _id}))
 
        (= invitation-status "expired")
        (-> (str utils/host-url "/objectives/" objective-id)
            response/redirect
            (assoc :flash "This invitation has expired"))

        :else (error-404-response request))

      (= status ::http-api/not-found) (error-404-response request)
      :else {:status 500})))

(defn remove-invitation-credentials [response]
  (update-in response [:session] dissoc :invitation))

(defn accept-or-decline-invitation [{:keys [session t' locale uri] :as request}]
  (if-let [invitation-details (:invitation session)]
    (if (= ::http-api/success (:status (http-api/retrieve-invitation-by-uuid (:uuid invitation-details))))
      (let [{objective :result} (http-api/get-objective (:objective-id invitation-details))]
        (views/invitation-response "invitation-response"
                                   request
                                   :objective (format-objective objective)))
      (-> (error-404-response request)
          (assoc :session session)
          remove-invitation-credentials)) 
    (error-404-response request)))

(defn accept-invitation [{:keys [session]}]
  (if-let [invitation-credentials (:invitation session)]
    (let [candidate-writer {:invitee-id (get (friend/current-authentication) :identity)
                            :invitation-uuid (:uuid invitation-credentials)
                            :objective-id (:objective-id invitation-credentials)}
          {status :status} (http-api/post-candidate-writer candidate-writer)]
      (cond
        (= status ::http-api/success)
        (-> (str utils/host-url "/objectives/" (:objective-id invitation-credentials) "#writers")
            response/redirect
            (assoc :session session)
            remove-invitation-credentials
            (utils/add-authorisation-role (utils/writer-for (:objective-id invitation-credentials))))
        
        :else {:status 500}))
    {:status 401}))

(defn decline-invitation [{session :session :as request}]
  (if-let [invitation-credentials (:invitation session)]
    (let [{status :status} (http-api/decline-invitation {:invitation-id (:invitation-id invitation-credentials)
                                                         :objective-id (:objective-id invitation-credentials)
                                                         :invitation-uuid (:uuid invitation-credentials)})] 
      (cond
       (#{::http-api/success ::http-api/invalid-input ::http-api/not-found} status)
       (-> (str utils/host-url)
                  response/redirect
                  (assoc :flash "Invitation declined")
                  (assoc :session session)
                  remove-invitation-credentials)
       
       :else {:status 500}))
    {:status 401}))

;;DRAFTS

(defn add-draft-get [{{objective-id :id} :route-params :as request}]
   (let [{objective-status :status objective :result} (http-api/get-objective (Integer/parseInt objective-id))]
     (cond
       (= objective-status ::http-api/success)
       (if (:drafting-started objective)
         (views/add-draft "add-draft" request :objective-id objective-id)
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
        (views/add-draft "add-draft" request :objective-id objective-id :preview preview :markdown content))

      (= action "submit")
      (let [{status :status draft :result} (http-api/post-draft {:objective-id objective-id
                                                                 :submitter-id (get (friend/current-authentication) 
                                                                                    :identity)
                                                                 :content parsed-markdown})]
        (cond
          (= status ::http-api/success) (response/redirect (str "/objectives/" o-id "/drafts/" (:_id draft)))    
          (= status ::http-api/not-found) {:status 404}
          :else {:status 502})))))

(defn draft [{{:keys [d-id id]} :route-params :as request}]
  (let [objective-id (Integer/parseInt id)
        draft-id (if (= d-id "latest")
                   d-id
                   (Integer/parseInt d-id))
        {objective-status :status objective :result} (http-api/get-objective objective-id)] 
    (if (= objective-status ::http-api/success)
      (let [{draft-status :status draft :result} (http-api/get-draft objective-id draft-id)
            {candidate-status :status candidates :result} (http-api/retrieve-candidates objective-id)] 
        (cond
          (every? #(= ::http-api/success %) [draft-status candidate-status])
          (let [draft-content (utils/hiccup->html (apply list (:content draft)))]
            {:status 200
             :body (views/draft "draft" request
                                :objective objective
                                :candidates candidates
                                :draft-content draft-content
                                :draft draft)
             :headers {"Content-Type" "text/html"}})

          (or (= draft-status ::http-api/forbidden) (= draft-status ::http-api/not-found)) 
          (if (= d-id "latest")
            {:status 200
             :body (views/draft "draft" request
                                :candidates candidates
                                :objective (format-objective objective))
             :headers {"Content-Type" "text/html"}}
            (error-404-response request))

          :else {:status 500}))
      (error-404-response request))))

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

(defn- redirect-to-params-referer [request]
  (let [location (utils/safen-url (get-in request [:params :refer] "/"))]
    (response/redirect location)))

(defn post-up-vote [request]
  (-> (helpers/request->up-vote-info request (get (friend/current-authentication) :identity))
      http-api/create-up-down-vote)
  (redirect-to-params-referer request))

(defn post-down-vote [request]
  (-> (helpers/request->down-vote-info request (get (friend/current-authentication) :identity))
      http-api/create-up-down-vote)
  (redirect-to-params-referer request))

