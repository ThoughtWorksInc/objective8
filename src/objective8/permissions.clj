(ns objective8.permissions
  (:require [cemerick.friend :as friend]))

(defn writer-inviter-for [objective-id]
  (keyword (str "writer-inviter-for-" objective-id)))

(defn writer-inviter-for? [user objective-id]
  (contains? (:roles user) (writer-inviter-for objective-id)))

(defn writer-for [objective-id]
  (keyword (str "writer-for-" objective-id)))

(defn writer-for? [user objective-id]
  (contains? (:roles user) (writer-for objective-id)))

(defn writer? [user]
  (some #(re-matches #":writer-for-\d+$|:owner-of-\d+$" (str %)) (:roles user)))

(defn owner-of [objective-id]
  (keyword (str "owner-of-" objective-id)))

(defn owner-of? [objective-id user]
  (contains? (:roles user) (owner-of objective-id)))

(defn admin? [user]
  (contains? (:roles user) :admin))

(defn can-mark-questions? [objective user]
  (let [roles (:roles user)
        objective-id (:_id objective)]
    (or (writer-for? user objective-id)
        (owner-of? objective-id user))))

(defn mark-request->mark-question-roles [{:keys [params] :as request}]
  (let [objective-id (second (re-matches #"/objectives/(\d+)/questions/\d+" (:question-uri params)))]
   #{(writer-for objective-id)
     (owner-of objective-id)}))

(defn request->writer-roles [{:keys [route-params] :as request}]
  #{(writer-for (:id route-params))})

(defn add-authorisation-role
  "If the session in the request-or-response is already authenticated,
  then adds a new-role to the list of authorised roles, otherwise
  returns the request-or-response."
  [request-or-response new-role]
  (if-let [new-authentication (some-> (friend/current-authentication request-or-response)
                                      (update-in [:roles] conj new-role))]
    (friend/merge-authentication request-or-response new-authentication)
    request-or-response))

