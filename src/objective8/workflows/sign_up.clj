(ns objective8.workflows.sign-up
  (:require [cemerick.friend.workflows :as workflows]
            [cemerick.friend :as friend]
            [ring.util.response :as response]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [bidi.ring :refer [make-handler]]
            [objective8.config :as config]
            [objective8.http-api :as http-api]
            [objective8.utils :as utils]
            [objective8.handlers.front-end :as front-end]))

(def sign-up-routes
  ["/" {"sign-up" {:get :sign-up-form
                   :post :sign-up-form-post}}])

(defn auth-map [user]
  (workflows/make-auth {:username (:_id user) :roles #{:signed-in}}
                       {::friend/workflow :objective8.workflows.sign-up/sign-up-workflow}))

(defn authorise [response user]
  (friend/merge-authentication response (auth-map user)))

(defn authorised-redirect [user redirect-url current-session]
  (-> (response/redirect redirect-url)
      (assoc :session (select-keys current-session [:invitation]))
      (authorise user)))

(defn finalise-authorisation [user current-session]
  (if-let [redirect-url (utils/safen-url (:sign-in-referrer current-session))]
    (authorised-redirect user redirect-url current-session)
    (auth-map user)))

(defn sign-up-form [{session :session :as request}]
  (if-let [twitter-id (:twitter-id session)]
    (let [{status :status user :result} (http-api/find-user-by-twitter-id twitter-id)]
      (cond
        (= status ::http-api/success) (finalise-authorisation user session)
        (= status ::http-api/not-found) (front-end/sign-up-form request)
        (= status ::http-api/invalid-input) {:status 400}
        :else {:status 500}))
    (response/redirect "/sign-in")))

(defn sign-up-form-post [{params :params session :session :as request}]
  (if-let [twitter-id (:twitter-id session)]
    (let [twitter-screen-name (:twitter-screen-name session)
          email-address (:email-address params) 
          {status :status user :result} (http-api/create-user {:twitter-id twitter-id
                                                               :display-name twitter-screen-name
                                                               :email-address email-address})]
      (cond
        (= status ::http-api/success) (finalise-authorisation user session)
        (= status ::http-api/invalid-input) {:status 400}
        :else {:status 502}))
    {:status 401}))

(def sign-up-handlers
  {:sign-up-form        (utils/anti-forgery-hook sign-up-form)
   :sign-up-form-post   (utils/anti-forgery-hook sign-up-form-post)})

(def sign-up-workflow
  (make-handler sign-up-routes sign-up-handlers))
