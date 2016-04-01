(ns objective8.middleware
  (:require [clojure.string :as s]
            [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [objective8.front-end.permissions :as permissions]))

(defn- keywordize [m]
  (into {} (for [[k v] m] [(keyword k) v])))

(defn valid-credentials? [tokens-fn bearer-name provided-token]
  (when bearer-name
    (when-let [stored-token (tokens-fn bearer-name)]
      (= provided-token stored-token))))

(defn wrap-bearer-token
  "Middleware for authorising api requests.
  Tokens-fn should be a function that takes
  bearer name and returns a token or nil."
  [handler tokens-fn]
  (fn [{:keys [headers] :as request}]
    (let [bearer-token (get headers "api-bearer-token")
          bearer-name (get headers "api-bearer-name")]
      (if (valid-credentials? tokens-fn bearer-name bearer-token)
        (handler (update-in request [:headers] dissoc "api-bearer-token" "api-bearer-name"))
        (do (log/info (str "Invalid bearer token credentials"))
            {:status 401})))))

(defn wrap-not-found [handler not-found-handler]
  (fn [request]
    (if-let [response (handler request)]
      response
      (not-found-handler request))))

(defn strip-trailing-slashes [handler]
  (fn [request]
    (handler (update-in request [:uri] s/replace #"(.)/$" "$1"))))

(defn wrap-authorise-writer-inviter [handler]
  (fn [{:keys [route-params] :as request}]
    (let [roles #{(permissions/writer-inviter-for (:id route-params))}]
      (if (friend/authorized? roles (friend/identity request))
        (handler request)
        (friend/throw-unauthorized (friend/identity request)
                                   {::friend/wrapped-handler handler
                                    ::friend/required-roles roles})))))

(defn authorize-based-on-request [handler request->roles]
  (fn [request]
    (let [roles (request->roles request)]
      (if (friend/authorized? roles (friend/identity request))
        (handler request)
        (friend/throw-unauthorized (friend/identity request)
                                   {::friend/wrapped-handler handler
                                    ::friend/required-roles roles})))))

(defn wrap-handlers-except [handlers wrap-function exclusions]
  (into {} (for [[k v] handlers]
             [k (if (k exclusions) v (wrap-function v))])))

(defn wrap-just-these-handlers [handlers-m wrap-function inclusions]
  (into {} (for [[k v] handlers-m]
             [k (if (k inclusions) (wrap-function v) v)])))