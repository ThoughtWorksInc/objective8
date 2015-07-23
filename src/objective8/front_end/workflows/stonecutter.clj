(ns objective8.front-end.workflows.stonecutter
  (:require [ring.util.response :as response]
            [clojure.tools.logging :as log]
            [bidi.ring :as bidi]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [stonecutter-oauth.client :as soc]
            [objective8.front-end.views :as views]
            [objective8.utils :as utils]))

(defn invalid-configuration-handler [_]
  (response/redirect (utils/path-for :fe/error-configuration)))

(defn stonecutter-sign-in [{:keys [stonecutter-config] :as request}]
  (soc/authorisation-redirect-response stonecutter-config))

(defn stonecutter-callback [request]
  (try
    (let [auth-code (get-in request [:params :code])
          stonecutter-config (get-in request [:stonecutter-config])
          token-response (soc/request-access-token! stonecutter-config auth-code)
          auth-provider-user-id (str "d-cent-" (:user-id token-response))]
      (-> (response/redirect (str utils/host-url "/sign-up"))
          (assoc-in [:session :auth-provider-user-id] auth-provider-user-id)))
    (catch Exception e
      (do (log/info "Exception in Stonecutter callback handler: " e)
          (invalid-configuration-handler request)))))

(defn wrap-stonecutter-config [handler config invalid-configuration-handler]
  (case config
    :invalid-configuration invalid-configuration-handler
    (fn [request] (handler (assoc request :stonecutter-config config)))))

(def stonecutter-routes
  ["/" {"d-cent-sign-in" :sign-in
        "d-cent-callback" :callback}])

(defn stonecutter-handlers [config]
  {:sign-in (wrap-stonecutter-config stonecutter-sign-in config invalid-configuration-handler)
   :callback (wrap-stonecutter-config stonecutter-callback config invalid-configuration-handler)})

(defn workflow [config]
  (bidi/make-handler stonecutter-routes (stonecutter-handlers config)))
