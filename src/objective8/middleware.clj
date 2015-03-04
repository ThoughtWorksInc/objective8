(ns objective8.middleware
  (:require [clojure.string :as s]))

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
        {:status 401}))))

(defn wrap-not-found [handler not-found-handler]
  (fn [request]
    (if-let [response (handler request)]
      response
      (not-found-handler request))))

(defn strip-trailing-slashes [handler]
  (fn [request]
    (handler (update-in request [:uri] s/replace #"(.)/$" "$1"))))
