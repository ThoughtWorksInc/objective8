(ns objective8.front-end.workflows.sign-up
  (:require [cemerick.friend.workflows :as workflows]
            [cemerick.friend :as friend]
            [ring.util.response :as response]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.flash :refer [wrap-flash]]
            [bidi.ring :refer [make-handler]]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.front-end.api.http :as http-api]
            [objective8.front-end.front-end-requests :as fr]
            [objective8.front-end.permissions :as permissions]
            [objective8.front-end.views :as views]))

(def sign-up-routes
  ["/" {"sign-up" {:get :sign-up-form
                   :post :sign-up-form-post}}])


(defn roles-for-user [user]
  (let [{{:keys [writer-records owned-objectives admin]} :result :as g-user} (http-api/get-user (:_id user))
        writer-objective-ids (map :objective-id writer-records)
        owned-objective-ids (map :_id owned-objectives)
        writer-roles (map permissions/writer-for writer-objective-ids)
        writer-inviter-roles (->> writer-objective-ids
                                  (concat owned-objective-ids)
                                  (map permissions/writer-inviter-for))
        objective-owner-roles (map permissions/owner-of owned-objective-ids)
        admin-role (if admin [:admin] [])] 
        
    (-> (concat writer-roles writer-inviter-roles objective-owner-roles admin-role)
        (conj :signed-in)
        set)))

(defn auth-map [user]
  (workflows/make-auth {:identity (:_id user) :roles (roles-for-user user) :username (:username user)}
                       {::friend/workflow :objective8.front-end.workflows.sign-up/sign-up-workflow}))

(defn authorise [response user]
  (friend/merge-authentication response (auth-map user)))

(defn authorised-redirect [user redirect-url current-session]
  (-> (response/redirect redirect-url)
      (assoc :session (select-keys current-session [:invitation]))
      (authorise user)))

(defn finalise-authorisation [user current-session]
  (if-let [redirect-url (some-> current-session :sign-in-referrer utils/safen-url)]
    (authorised-redirect user redirect-url current-session)
    (auth-map user)))

(defn sign-up-form [{session :session :as request}]
  (if-let [twitter-id (:twitter-id session)]
    (let [{status :status user :result :as td} (http-api/find-user-by-twitter-id twitter-id)]
      (cond
        (= status ::http-api/success) (finalise-authorisation user session)
        (= status ::http-api/not-found) {:status 200
                                         :header {"Content-Type" "text/html"}  
                                         :body (views/sign-up "sign-up" request)}
        (= status ::http-api/invalid-input) {:status 400}
        :else {:status 500}))
    (response/redirect "/sign-in")))

(defn sign-up-form-post [{:keys [params session] :as request}]
  (if-let [twitter-id (:twitter-id session)]
    (let [user-sign-up-data (fr/request->user-sign-up-data request)]
      (case (:status user-sign-up-data)
        ::fr/valid
        (let [{status :status user :result} (http-api/create-user 
                                              {:twitter-id twitter-id
                                               :username (-> user-sign-up-data :data :username)
                                               :email-address (-> user-sign-up-data :data :email-address)})]
          (case status
            ::http-api/success (finalise-authorisation user session)
            ::http-api/invalid-input (-> (response/redirect (str utils/host-url "/sign-up"))
                                         (assoc :flash {:validation 
                                                        (-> user-sign-up-data
                                                            (assoc :report {:username #{:duplicated}})
                                                            (dissoc :status))})) 
            {:status 502})) 

        ::fr/invalid
        (-> (response/redirect (str utils/host-url "/sign-up"))
            (assoc :flash {:validation (dissoc user-sign-up-data :status)}))))
    {:status 401}))

(def sign-up-handlers
  {:sign-up-form        (utils/anti-forgery-hook sign-up-form) 
   :sign-up-form-post   (utils/anti-forgery-hook sign-up-form-post)})

(def sign-up-workflow
  (make-handler sign-up-routes sign-up-handlers))
