(ns d-cent.workflows.sign-up
  (:require [cemerick.friend.workflows :as workflows]
            [cemerick.friend :as friend]
            [ring.util.response :as response]
            [bidi.ring :refer [make-handler]]
            [d-cent.http-api :as api]
            [d-cent.user :as user]
            [d-cent.handlers.front-end :as front-end]))

(def sign-up-routes
  ["/" {"sign-up" {:get :sign-up-form
                   :post :sign-up-form-post}}])

(defn sign-up-form [{session :session :as request}]
  (if-let [user-id (:d-cent-user-id session)]
    (if-let [user-profile (api/find-user-profile-by-twitter-id user-id)]
      (workflows/make-auth {:username user-id :roles #{:signed-in}}
                           {::friend/workflow :d-cent.workflows.sign-up/sign-up-workflow})
      (front-end/sign-up-form request))
    (response/redirect "/sign-in")))

(defn sign-up-form-post [{params :params session :session :as request}]
  (if-let [user-id (:d-cent-user-id session)]
    (let [email-address (:email-address params)]
      (api/create-user-profile {:user-id user-id :email-address email-address})
      (workflows/make-auth {:username user-id :roles #{:signed-in}}
                           {::friend/workflow :d-cent.workflows.sign-up/sign-up-workflow}))
    {:status 401}))

(def sign-up-handlers
  {:sign-up-form        sign-up-form
   :sign-up-form-post   sign-up-form-post})

(def sign-up-workflow
  (make-handler sign-up-routes sign-up-handlers))
