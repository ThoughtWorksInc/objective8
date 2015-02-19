(ns objective8.handlers.front-end
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [org.httpkit.client :as http]
            [objective8.responses :refer :all]
            [objective8.http-api :as http-api]
            [objective8.front-end-helpers :refer [request->question request->comment 
                                                  request->objective request->answer-info]]
            [objective8.utils :as utils]
            [objective8.storage.storage :as storage]))

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

;; USER PROFILE

(defn sign-up-form [{:keys [t' locale]}]
  (rendered-response users-email {:translation t'
                                  :locale (subs (str locale) 1)
                                  :doc-title (t' :users-email/doc-title)
                                  :doc-description (t' :users-email/doc-description)
                                  :signed-in (signed-in?)}))

;; OBJECTIVES
(defn objective-list [{:keys [t' locale]}]
  (rendered-response objective-list-page {:translation t'
                                          :locale (subs (str locale) 1)
                                          :doc-title (t' :objective-list/doc-title)
                                          :doc-description (t' :objective-list/doc-description)
                                          :signed-in (signed-in?)}))

(defn create-objective-form [{:keys [t' locale]}]
  (rendered-response objective-create-page {:translation t'
                                            :locale (subs (str locale) 1)
                                            :doc-title (t' :objective-create/doc-title)
                                            :doc-description (t' :objective-create/doc-description)
                                            :signed-in (signed-in?)}))

(defn create-objective-form-post [{:keys [t' locale] :as request}]
    (if-let [objective (request->objective request (get (friend/current-authentication) :username))]
      (if-let [stored-objective (http-api/create-objective objective)]
        (let [objective-url (str utils/host-url "/objectives/" (:_id stored-objective))
              message (t' :objective-view/created-message)]
          (assoc (response/redirect objective-url) :flash message))
        {:status 502})
      {:status 400}))

(defn objective-detail [{{id :id} :route-params
                         message :flash
                         :keys [uri t' locale]
                         :as request}]
  (try (let [objective-id (Integer/parseInt id)
        objective (http-api/get-objective objective-id)]
    (if (= (objective :status) 404)
        (error-404-response t' locale)
        (let [comments (http-api/retrieve-comments objective-id)
              goals (if (objective :goals)
                       (list (objective :goals))
                       (remove clojure.string/blank? [(:goal-1 objective) (:goal-2 objective) (:goal-3 objective)]))
              formatted-objective (-> objective
                                    (update-in [:end-date] utils/date-time->pretty-date)
                                    (assoc :goals goals)
                                    (dissoc :goal-1 :goal-2 :goal-3))]
            (rendered-response objective-detail-page {:translation t'
                                                    :locale (subs (str locale) 1)
                                                    :doc-title (str (:title objective) " | Objective[8]")
                                                    :doc-description (:title objective)
                                                    :message message
                                                    :objective formatted-objective
                                                    :comments comments
                                                    :signed-in (signed-in?)
                                                    :uri uri}))))
       (catch NumberFormatException e
         (log/info "Invalid route: " e)
         (error-404-response t' locale))))

;; COMMENTS

(defn create-comment-form-post [{{objective-id :objective-id} :params
                                 :keys [t' locale] :as request}]
  (if-let [comment (request->comment request (get (friend/current-authentication) :username))]
    (if-let [stored-comment (http-api/create-comment comment)]
      (let [comment-url (str utils/host-url "/objectives/" (:objective-id stored-comment))
            message (t' :comment-view/created-message)]
        (assoc (response/redirect comment-url) :flash message))
      {:status 502})
    {:status 400}))

;; QUESTIONS

(defn add-question-form [{{id :id} :route-params
                          :keys [t' locale]}]
  (let [objective-id (Integer/parseInt id)
        objective (http-api/get-objective objective-id)]
    (if (= (objective :status) 404)
      (error-404-response t' locale)
      (rendered-response question-add-page {:translation t'
                                            :locale (subs (str locale) 1)
                                            :doc-title (t' :question-add/doc-title)
                                            :doc-description (t' :question-add/doc-description)
                                            :objective-title (:title objective)
                                            :objective-id (:_id objective)
                                            :signed-in (signed-in?)}))))

(defn add-question-form-post [{:keys [uri t' locale] :as request}]
  (if-let [question (request->question request (get (friend/current-authentication) :username))]
    (if-let [stored-question (http-api/create-question question)]
      (let [question-url (str utils/host-url "/objectives/" (:objective-id stored-question) "/questions/" (:_id stored-question))
            message (t' :question-view/added-message)]
        (assoc (response/redirect question-url) :flash message))
      {:status 502})
    {:status 400}))

(defn question-detail [{{q-id :q-id id :id} :route-params
                         message :flash
                         :keys [uri t' locale]
                         :as request}]
  (let [{status :status question :result} (http-api/get-question (Integer/parseInt id) (Integer/parseInt q-id))
        answers (http-api/retrieve-answers  (:objective-id question) (:_id question))]
    (cond
      (= status ::http-api/success)
          (rendered-response question-view-page {:translation t'
                                                 :locale (subs (str locale) 1)
                                                 :doc-title (str (:title question) " | Objective[8]")
                                                 :doc-description (:title question)
                                                 :message message
                                                 :question question
                                                 :answers answers
                                                 :signed-in (signed-in?)
                                                 :uri uri})
      (= status ::http-api/not-found) (error-404-response t' locale)
      :else {:status 500})))


;; ANSWERS

(defn add-answer-form-post [{:keys [uri t' locale] :as request}]
  (if-let [answer (request->answer-info request (get (friend/current-authentication) :username))]
    (let [{status :status stored-answer :result} (http-api/create-answer answer)]
      (cond
        (= status ::http-api/success)
        (let [answer-url (str utils/host-url "/objectives/" (:objective-id stored-answer)
                              "/questions/" (:question-id stored-answer))
              message (t' :question-view/added-answer-message)]
          (assoc (response/redirect answer-url) :flash message))
        :else {:status 502}))
    {:status 400}))




