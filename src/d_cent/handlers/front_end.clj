(ns d-cent.handlers.front-end
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [org.httpkit.client :as http]
            [d-cent.responses :refer :all]
            [d-cent.objectives :refer [request->objective find-by-id]]
            [d-cent.http-api :as http-api]
            [d-cent.utils :as utils]
            [d-cent.storage :as storage]))

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

;; user profile

(defn sign-up-form [{:keys [t' locale]}]
  (rendered-response users-email {:translation t'
                                  :locale (subs (str locale) 1)
                                  :doc-title (t' :users-email/doc-title)
                                  :doc-description (t' :users-email/doc-description)
                                  :signed-in (signed-in?)}))

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
              message (str (t' :objective-view/created-message) " <a title='"(t' :objective-view/share-title)"' href='http://twitter.com/share?text="(t' :objective-view/share-twitter-text)"&url=" objective-url "''>"(t' :objective-view/share-text)"</a>")]
          (assoc (response/redirect objective-url) :flash message))
        {:status 502})
      {:status 400}))

(defn objective-detail [{{guid :id} :route-params
                         message :flash
                         :keys [t' locale]
                         :as request}]
  (let [objective (http-api/get-objective guid)]
    (rendered-response objective-view-page {:translation t'
                                            :locale (subs (str locale) 1)
                                            :doc-title (t' :objective-view/doc-title)
                                            :doc-description (t' :objective-view/doc-description)
                                            :message message
                                            :objective (update-in objective [:end-date] utils/time-string->pretty-date)
                                            :signed-in (signed-in?)})))
