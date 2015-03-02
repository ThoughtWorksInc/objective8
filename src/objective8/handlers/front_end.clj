(ns objective8.handlers.front-end
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [org.httpkit.client :as http]
            [objective8.responses :refer :all]
            [objective8.http-api :as http-api]
            [objective8.front-end-helpers :as helpers]
            [objective8.utils :as utils]))

;; HELPERS

(defn signed-in? []
  (friend/authorized? #{:signed-in} friend/*identity*))

(defn error-404-response [t' locale]
  (rendered-response error-404-page {:translation t'
                                     :locale (subs (str locale) 1)
                                     :signed-in (signed-in?)
                                     :status-code 404
                                     :objectives-nav false}))
;; HANDLERS

(defn error-404 [{:keys [t' locale]}]
  (error-404-response t' locale))

(defn index [{:keys [t' locale] :as request}]
  (rendered-response index-page {:translation t'
                                 :locale (subs (str locale) 1)
                                 :doc-title (t' :index/doc-title)
                                 :doc-description (t' :index/doc-description)
                                 :signed-in (signed-in?)}))

(defn sign-in [{{refer :refer} :params
                :keys [t' locale]
                :as request}]
  (-> (rendered-response sign-in-page {:translation t'
                                       :locale (subs (str locale) 1)
                                       :doc-title (t' :sign-in/doc-title)
                                       :doc-description (t' :sign-in/doc-description)
                                       :signed-in (signed-in?)})
      (assoc :session (:session request))
      (assoc-in [:session :sign-in-referrer] refer)))

(defn sign-out [request]
  (assoc
   (friend/logout* (response/redirect "/"))
   :session {}))


(defn project-status [{:keys [t' locale]}]
  (rendered-response project-status-page {:translation t'
                                          :locale (subs (str locale) 1)
                                          :doc-title (t' :project-status/doc-title)
                                          :doc-description (t' :project-status/doc-description)
                                          :signed-in (signed-in?)}))

(defn learn-more [{:keys [t' locale]}]
  (rendered-response learn-more-page {:translation t'
                                      :locale (subs (str locale) 1)
                                      :doc-title (t' :learn-more/doc-title)
                                      :doc-description (t' :learn-more/doc-description)
                                      :signed-in (signed-in?)}))
;; USER PROFILE

(defn sign-up-form [{:keys [t' locale]}]
  (rendered-response users-email {:translation t'
                                  :locale (subs (str locale) 1)
                                  :doc-title (t' :users-email/doc-title)
                                  :doc-description (t' :users-email/doc-description)
                                  :signed-in (signed-in?)}))

;; OBJECTIVES
(defn format-objective [objective]
  (let [goals (if (objective :goals)
                (list (objective :goals))
                (remove clojure.string/blank? [(:goal-1 objective) (:goal-2 objective) (:goal-3 objective)]))
        formatted-objective (-> objective
                                (update-in [:end-date] utils/date-time->pretty-date)
                                (assoc :goals goals)
                                (dissoc :goal-1 :goal-2 :goal-3))]
    formatted-objective))

(defn objective-list [{:keys [t' locale]}]
  (let [{status :status objectives :result} (http-api/get-all-objectives)]
    (cond 
      (= status ::http-api/success) (rendered-response objective-list-page 
                                                       {:objectives (map format-objective objectives)
                                                        :translation t'
                                                        :locale (subs (str locale) 1)
                                                        :doc-title (t' :objective-list/doc-title)
                                                        :doc-description (t' :objective-list/doc-description)
                                                        :signed-in (signed-in?)})
      (= status ::http-api/error)   {:status 502})))

(defn create-objective-form [{:keys [t' locale]}]
  (rendered-response objective-create-page {:translation t'
                                            :locale (subs (str locale) 1)
                                            :doc-title (t' :objective-create/doc-title)
                                            :doc-description (t' :objective-create/doc-description)
                                            :signed-in (signed-in?)}))

(defn create-objective-form-post [{:keys [t' locale] :as request}]
  (if-let [objective (helpers/request->objective request (get (friend/current-authentication) :username))]
    (let [{status :status stored-objective :result} (http-api/create-objective objective)]
      (cond (= status ::http-api/success)
            (let [objective-url (str utils/host-url "/objectives/" (:_id stored-objective))
                  message (t' :objective-view/created-message)]
              (assoc (response/redirect objective-url) :flash message)) 

            (= status ::http-api/invalid-input) {:status 400}

            :else {:status 502}))
    {:status 400}))

(defn objective-detail [{{id :id} :route-params
                         message :flash
                         :keys [uri t' locale]
                         :as request}]
  (try (let [objective-id (Integer/parseInt id)
             {objective-status :status objective :result} (http-api/get-objective objective-id)
             {comments-status :status comments :result} (http-api/retrieve-comments objective-id)]
         (cond
           (every? #(= ::http-api/success %) [objective-status comments-status])
           (let [formatted-objective (format-objective objective)]
             (rendered-response objective-detail-page {:translation t'
                                                       :locale (subs (str locale) 1)
                                                       :doc-title (str (:title objective) " | Objective[8]")
                                                       :doc-description (:title objective)
                                                       :message message
                                                       :objective formatted-objective
                                                       :comments comments
                                                       :signed-in (signed-in?)
                                                       :uri uri}))
           (= objective-status ::http-api/not-found) (error-404-response t' locale)

           (= objective-status ::http-api/invalid-input) {:status 400}

           :else {:status 500}))
       (catch NumberFormatException e
         (log/info "Invalid route: " e)
         (error-404-response t' locale))))

;; COMMENTS

(defn create-comment-form-post [{{objective-id :objective-id} :params
                                 :keys [t' locale] :as request}]
  (if-let [comment (helpers/request->comment request (get (friend/current-authentication) :username))]
    (let [{status :status stored-comment :result} (http-api/create-comment comment)]
      (cond
        (= status ::http-api/success)
        (let [comment-url (str utils/host-url "/objectives/" (:objective-id stored-comment))
              message (t' :comment-view/created-message)]
          (assoc (response/redirect comment-url) :flash message))

        (= status ::http-api/invalid-input) {:status 400}

        :else {:status 502}))
    {:status 400}))

;; QUESTIONS

(defn question-list [{{id :id} :route-params
                          :keys [uri t' locale]}]
  (try 
    (let [objective-id (Integer/parseInt id)
          {objective-status :status objective :result} (http-api/get-objective objective-id)
          {questions-status :status questions :result} (http-api/retrieve-questions objective-id)]
      (cond
        (every? #(= ::http-api/success %) [objective-status questions-status])
        (rendered-response question-list-page {:translation t'
                                               :locale (subs (str locale) 1)
                                               :doc-title (str (:title objective) " | Objective[8]")
                                               :doc-description (str (t' :question-list/questions-about) " " (:title objective)) 
                                               :objective objective
                                               :questions questions
                                               :uri uri
                                               :signed-in (signed-in?)})
        (= objective-status ::http-api/not-found) (error-404-response t' locale)
        (= questions-status ::http-api/not-found) (error-404-response t' locale)
        (= questions-status ::http-api/invalid-input) {:status 400}
        :else {:status 500}))
    (catch NumberFormatException e
      (log/info "Invalid route: " e)
      (error-404-response t' locale))))

(defn add-question-form-post [{:keys [uri t' locale] :as request}]
  (if-let [question (helpers/request->question request (get (friend/current-authentication) :username))]
    (let [{status :status stored-question :result} (http-api/create-question question)]
      (cond 
        (= status ::http-api/success)
        (let [question-url (str utils/host-url "/objectives/" (:objective-id stored-question) "/questions/" (:_id stored-question))
              message (t' :question-view/added-message)]
          (assoc (response/redirect question-url) :flash message))

        (= status ::http-api/invalid-input) {:status 400}

        :else {:status 502}))
    {:status 400}))

(defn question-detail [{{q-id :q-id id :id} :route-params
                        message :flash
                        :keys [uri t' locale]
                        :as request}]
  (try (let [{question-status :status question :result} (http-api/get-question (Integer/parseInt id) (Integer/parseInt q-id))
             {answer-status :status answers :result} (http-api/retrieve-answers (:objective-id question) (:_id question))
             {objective-status :status objective :result} (http-api/get-objective (:objective-id question))]
         (cond
           (every? #(= ::http-api/success %) [question-status answer-status objective-status])
           (rendered-response question-view-page {:translation     t'
                                                  :locale          (subs (str locale) 1)
                                                  :doc-title       (str (:question question) " | Objective[8]")
                                                  :doc-description (:question question)
                                                  :message         message
                                                  :question        question
                                                  :answers         answers
                                                  :objective       objective
                                                  :signed-in       (signed-in?)
                                                  :uri             uri})
           (= question-status ::http-api/not-found) (error-404-response t' locale)
           (= question-status ::http-api/invalid-input) {:status 400}
           :else {:status 500}))
       (catch NumberFormatException e
         (log/info "Invalid route: " e)
         (error-404-response t' locale))))


;; ANSWERS

(defn add-answer-form-post [{:keys [uri t' locale] :as request}]
  (if-let [answer (helpers/request->answer-info request (get (friend/current-authentication) :username))]
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

(defn candidate-list [{{id :id} :route-params
                       :keys [uri t' locale]}]
  (try
    (let [objective-id (Integer/parseInt id)
          {objective-status :status objective :result} (http-api/get-objective objective-id)
          {candidate-status :status  candidates :result} (http-api/retrieve-candidates objective-id)]
      (cond
        (every? #(= ::http-api/success %) [candidate-status objective-status])
        (rendered-response candidate-list-page {:translation t'
                                                :locale (subs (str locale) 1)
                                                :doc-title (t' :invitation/doc-title)
                                                :doc-description (t' :invitation/doc-description)
                                                :objective objective
                                                :candidates candidates
                                                :uri uri
                                                :signed-in (signed-in?)})
        (= objective-status ::http-api/not-found) (error-404-response t' locale)
        (= candidate-status ::http-api/not-found) (error-404-response t' locale)
        (= candidate-status ::http-api/invalid-input) {:status 400}
        :else {:status 500}))
    (catch NumberFormatException e
      (log/info "Invalid route: " e)
      (error-404-response t' locale))))

(defn invitation-form-post [{:keys [t' locale] :as request}]
  (if-let [invitation (helpers/request->invitation-info request (get (friend/current-authentication) :username))]
    (let [{status :status stored-invitation :result} (http-api/create-invitation invitation)]
      (cond
        (= status ::http-api/success)
        (let [invitation-url (str utils/host-url "/objectives/" (:objective-id stored-invitation))
              message (str "Your invited writer can accept their invitation by going to "
                           utils/host-url "/invitations/" (:uuid stored-invitation))]
          (assoc (response/redirect invitation-url) :flash message))
        (= status ::http-api/invalid-input) {:status 400}
        :else {:status 502}))
    {:status 400}))

(defn writer-invitation [{{uuid :uuid} :route-params :keys [t' locale session]}]
  (let [{status :status {:keys [objective-id _id]} :result} (http-api/retrieve-invitation-by-uuid uuid)]
    (cond
      (= status ::http-api/success)
      (-> (str utils/host-url "/objectives/" objective-id "/invited-writers/" _id)
          response/redirect
          (assoc :session session)
          (assoc-in [:session :invitation] {:uuid uuid :objective-id objective-id :invitation-id _id}))
      (= status ::http-api/not-found) (error-404-response t' locale)
      :else {:status 500})))

(defn accept-or-reject-invitation [{:keys [session t' locale uri] :as request}]
  (if-let [invitation-details (:invitation session)]
    (let [{objective :result} (http-api/get-objective (:objective-id invitation-details))]
      (rendered-response invitation-response-page {:translation t'
                                                   :locale (subs (str locale) 1)
                                                   :doc-title (t' :invitation-response/doc-title)
                                                   :doc-description (t' :invitation-response/doc-description)
                                                   :objective objective
                                                   :uri uri
                                                   :signed-in (signed-in?)}))
    (error-404-response t' locale)))

(defn remove-invitation-credentials [response current-session]
  (assoc response :session (dissoc current-session :invitation)))

(defn accept-invitation [{:keys [session]}]
  (prn (str "request: " session))
  (if-let [invitation-credentials (:invitation session)]
    (let [invitation-response {:invitee-id (get (friend/current-authentication) :username)
                               :invitation-id (:invitation-id invitation-credentials)
                               :objective-id (:objective-id invitation-credentials)}] 
      (http-api/accept-invitation invitation-response)
      (-> (str utils/host-url "/objectives/" (:objective-id invitation-credentials) "/candidate-writers")
          response/redirect
          (remove-invitation-credentials session)))
    {:status 401}))
