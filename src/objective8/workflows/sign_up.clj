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

(defn sign-up-form [{session :session :as request}]
  (if-let [twitter-id (:twitter-id session)]
    (if-let [user (http-api/find-user-by-twitter-id twitter-id)]
      (workflows/make-auth {:username (:_id user) :roles #{:signed-in}}
                           {::friend/workflow :objective8.workflows.sign-up/sign-up-workflow})
      (front-end/sign-up-form request))
    (response/redirect "/sign-in")))

(defn sign-up-form-post [{params :params session :session :as request}]
  (if-let [twitter-id (:twitter-id session)]
    (let [email-address (:email-address params)
          user (http-api/create-user {:twitter-id twitter-id :email-address email-address})
          auth (workflows/make-auth {:username (:_id user) :roles #{:signed-in}}
                                    {::friend/workflow :objective8.workflows.sign-up/sign-up-workflow})]
      (if-let [redirect-uri (utils/safen-url (:sign-in-referrer session))]
        (friend/merge-authentication (response/redirect redirect-uri) auth) 
        auth))
    {:status 401}))

(def sign-up-handlers
  {:sign-up-form        (utils/anti-forgery-hook sign-up-form)
   :sign-up-form-post   (utils/anti-forgery-hook sign-up-form-post)})

(def sign-up-workflow
  (make-handler sign-up-routes sign-up-handlers))
