(ns objective8.middleware)

(defn- keywordize [m]
  (into {} (for [[k v] m] [(keyword k) v])))

(defn- valid-credentials? [tokens bearer-name bearer-token]
  (if-let [actual-token (get tokens (keyword bearer-name))]
    (= bearer-token actual-token)))

(defn wrap-bearer-token [handler tokens]
  (fn [{:keys [headers] :as request}] 

    (let [bearer-token (get headers "api-bearer-token")
          bearer-name (get headers "api-bearer-name")]
      (if (valid-credentials? tokens bearer-name bearer-token)
        (handler (update-in request [:headers] dissoc "api-bearer-token" "api-bearer-name"))
        {:status 401}))))

