(ns d-cent.handlers.front-end
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [org.httpkit.client :as http]
            [d-cent.responses :refer :all]
            [d-cent.objectives :refer [request->objective]]
            [d-cent.utils :as utils]
            [d-cent.storage :as storage]))

;; HELPERS

(defn signed-in? []
  (friend/authorized? #{:signed-in} friend/*identity*))

(defn new-objective-link [stored-objective]
  (str utils/host-url "/objectives/" (:_id stored-objective)))


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

;; user profile

(defn email-capture-get [{:keys [t' locale]}]
  (rendered-response users-email {:translation t'
                                  :locale (subs (str locale) 1)
                                  :doc-title (t' :users-email/doc-title)
                                  :doc-description (t' :users-email/doc-description)
                                  :signed-in (signed-in?)}))

(defn user-profile-post [request]
  (let [user-id (:username (friend/current-authentication))
        email-address (get-in request [:params :email-address])
        api-response @(http/post "http://localhost:8080/api/v1/users" 
                                 {:headers {"Content-Type" "application/json"} 
                                  :body (json/generate-string {:user-id user-id 
                                                               :email-address email-address})})]
    {:status 200 :body api-response}))

;; objectives

(defn objective-new-link-page [{:keys [t' locale]} stored-objective]
  (rendered-response objectives-new-link-page {:status-code 201
                                               :translation t'
                                               :locale (subs (str locale) 1)
                                               :doc-title (t' :objective-new-link/doc-title)
                                               :doc-description (t' :objective-new-link/doc-description)
                                               :stored-objective (new-objective-link stored-objective)
                                               :signed-in (signed-in?)}))

(defn objective-create [{:keys [t' locale]}]
  (rendered-response objective-create-page {:translation t'
                                            :locale (subs (str locale) 1)
                                            :doc-title (t' :objective-create/doc-title)
                                            :doc-description (t' :objective-create/doc-description)
                                            :signed-in (signed-in?)}))

(defn objective-create-post [request]
  (if (friend/authorized? #{:signed-in} friend/*identity*)
    (let [objective (request->objective request)]
      (if objective
        (let [stored-objective (storage/store! (storage/request->store request) "objectives" objective)]
          (objective-new-link-page request stored-objective))
        {:status 400
         :body "oops"}))
    {:status 401}))

(defn objective-view [{:keys [t' locale] :as request}]
  (let [objective (storage/find-by (storage/request->store request) "objectives" (-> request :route-params :id))]
    (rendered-response objective-view-page {:translation t'
                                            :locale (subs (str locale) 1)
                                            :doc-title (t' :objective-view/doc-title)
                                            :doc-description (t' :objective-view/doc-description)
                                            :objective objective
                                            :signed-in (signed-in?)})))
