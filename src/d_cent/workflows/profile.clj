(ns d-cent.workflows.profile 
  (:require [cemerick.friend.workflows :as workflows]
            [cemerick.friend :as friend]
            [clojure.pprint :as pprint]
            [bidi.ring :refer [make-handler]]
            [d-cent.http-api :as api]))

(def capture-profile-routes
  ["/" {"sign-up" {:get :sign-up-form
                   :post :sign-up-form-post}}])

(defn sign-up-form [{session :session}]
  (let [user-id (:d-cent-user-id session)]
    (if-let [user-profile (api/find-user-profile-by-user-id user-id)]
      (workflows/make-auth {:username user-id :roles #{:signed-in}}
                           {::friend/workflow :d-cent.workflows.profile/capture-profile-workflow})
      nil)))

(defn sign-up-form-post [{params :params session :session :as request}]
  (let [username (:d-cent-user-id session)
        email-address (:email-address params)]
    (api/create-user-profile {:user-id username :email-address email-address})
    (workflows/make-auth {:username username :roles #{:signed-in}}
                         {::friend/workflow :d-cent.workflows.profile/capture-profile-workflow})))

(def capture-profile-handlers
  {:sign-up-form        sign-up-form
   :sign-up-form-post   sign-up-form-post})

(def capture-profile-workflow
  (make-handler capture-profile-routes capture-profile-handlers))










