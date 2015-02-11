(ns objective8.handlers.front-end
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [org.httpkit.client :as http]
            [objective8.responses :refer :all]
            [objective8.comments :refer [request->comment]]
            [objective8.objectives :refer [request->objective]]
            [objective8.http-api :as http-api]
            [objective8.utils :as utils]
            [objective8.storage.storage :as storage]))

;; HELPERS

(defn signed-in? []
  (friend/authorized? #{:signed-in} friend/*identity*))


;; HANDLERS

(defn index [{:keys [t' locale]}]
  (rendered-response index-page {:translation t'
                                 :locale (subs (str locale) 1)
                                 :doc-title (t' :index/doc-title)
                                 :doc-description (t' :index/doc-description)
                                 :signed-in (signed-in?)}))

(defn sign-in [{:keys [t' locale]}]
  (rendered-response sign-in-page {:translation t'
                                   :locale (subs (str locale) 1)
                                   :doc-title (t' :sign-in/doc-title)
                                   :doc-description (t' :sign-in/doc-description)
                                   :signed-in (signed-in?)}))

(defn sign-out [_]
  (friend/logout* (response/redirect "/")))


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

(defn create-objective-form [{:keys [t' locale]}]
  (rendered-response objective-create-page {:translation t'
                                            :locale (subs (str locale) 1)
                                            :doc-title (t' :objective-create/doc-title)
                                            :doc-description (t' :objective-create/doc-description)
                                            :signed-in (signed-in?)}))

(defn create-objective-form-post [{:keys [t' locale] :as request}]
    (if-let [objective (request->objective request)]
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
  (let [objective-id (Integer/parseInt id)
        objective (http-api/get-objective objective-id)]
    (if (= (objective :status) 404)
        (response/not-found "")
        (let [comments (http-api/retrieve-comments objective-id)]
            (rendered-response objective-view-page {:translation t'
                                                    :locale (subs (str locale) 1)
                                                    :doc-title (t' :objective-view/doc-title)
                                                    :doc-description (t' :objective-view/doc-description)
                                                    :message message
                                                    :objective (update-in objective [:end-date] utils/date-time->pretty-date)
                                                    :comments comments
                                                    :signed-in (signed-in?)
                                                    :uri uri})))))

;; COMMENTS

(defn create-comment-form-post [{{objective-id :objective-id} :params
                                 :keys [t' locale] :as request}]
  (if-let [comment (request->comment request)]
    (if-let [stored-comment (http-api/create-comment comment)]
      (let [comment-url (str utils/host-url "/objectives/" (:objective-id comment))
            message (t' :comment-view/created-message)]
        (assoc (response/redirect comment-url) :flash message))
      {:status 502})
    {:status 400}))
