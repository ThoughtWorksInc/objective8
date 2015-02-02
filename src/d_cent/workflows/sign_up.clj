(ns d-cent.workflows.sign-up
  (:require [cemerick.friend.workflows :as workflows]
            [cemerick.friend :as friend]
            [clojure.pprint :as pprint]
            [bidi.ring :refer [make-handler]]
            [d-cent.http-api :as api]))

(def sign-up-routes
  ["/" {"sign-up" {:get :sign-up-form
                   :post :sign-up-form-post}}])

(defn sign-up-form [{session :session}]
  (let [user-id (:d-cent-user-id session)]
    (if-let [user-profile (api/find-user-profile-by-user-id user-id)]
      (workflows/make-auth {:username user-id :roles #{:signed-in}}
                           {::friend/workflow :d-cent.workflows.sign-up/sign-up-workflow})
      nil)))

(defn sign-up-form-post [{params :params session :session :as request}]
  (let [username (:d-cent-user-id session)
        email-address (:email-address params)]
    (api/create-user-profile {:user-id username :email-address email-address})
    (workflows/make-auth {:username username :roles #{:signed-in}}
                         {::friend/workflow :d-cent.workflows.sign-up/sign-up-workflow})))

(def sign-up-handlers
  {:sign-up-form        sign-up-form
   :sign-up-form-post   sign-up-form-post})

(def sign-up-workflow
  (make-handler sign-up-routes sign-up-handlers))










