(ns objective8.front-end.workflows.stonecutter
  (:require [ring.util.response :as response]
            [clojure.tools.logging :as log]
            [bidi.ring :as bidi]
            [stonecutter-oauth.client :as soc]
            [stonecutter-oauth.jwt :as so-jwt]
            [objective8.utils :as utils]))

(defn invalid-configuration-handler [_]
  (response/redirect (utils/path-for :fe/error-configuration)))

(defn stonecutter-sign-in [{:keys [stonecutter-config] :as request}]
  (soc/authorisation-redirect-response stonecutter-config))

(defn get-auth-jwks-url [stonecutter-config]
  (str (:auth-provider-url stonecutter-config) "/api/jwk-set"))

(defn stonecutter-callback [request]
  (try
    (let [auth-code (get-in request [:params :code])
          stonecutter-config (get-in request [:stonecutter-config])
          token-response (soc/request-access-token! stonecutter-config auth-code)
          auth-jwks-url (get-auth-jwks-url stonecutter-config)
          public-key-string (so-jwt/get-public-key-string-from-jwk-set-url auth-jwks-url)
          user-info (so-jwt/decode stonecutter-config (:id_token token-response) public-key-string)]
      (if-let [sub (:sub user-info)]
        (-> (response/redirect (str utils/host-url "/sign-up"))
            (assoc :session (:session request))
            (assoc-in [:session :auth-provider-user-id] (str "d-cent-" sub)))
        (throw (ex-info "'sub' is nil or missing from user-info record in token-response from stonecutter"
                        {:user-info-keys (keys user-info)}))))
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
