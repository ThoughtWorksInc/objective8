(ns objective8.front-end.workflows.stonecutter
  (:require [ring.util.response :as response]
            [bidi.ring :as bidi]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [objective8.utils :as utils]))

(defn authorisation-redirect-response [stonecutter-config]
  (let  [callback-uri  (:callback-uri stonecutter-config)
         oauth-authorisation-path  (str (:auth-provider-url stonecutter-config)
                                        "/authorisation?client_id=" (:client-id stonecutter-config)
                                        "&response_type=code&redirect_uri=" callback-uri)]
    (-> (response/redirect oauth-authorisation-path)
        (assoc-in  [:headers "accept"] "text/html"))))

(defn request-access-token! [stonecutter-config auth-code]
    (let [callback-uri (:callback-uri stonecutter-config)
          oauth-token-path (str (:auth-provider-url stonecutter-config) "/api/token")
          token-response @(http/post oauth-token-path {:form-params  {:grant_type    "authorization_code"
                                                                      :redirect_uri  callback-uri
                                                                      :code          auth-code
                                                                      :client_id     (:client-id stonecutter-config)
                                                                      :client_secret (:client-secret stonecutter-config)}})]
      (-> token-response :body (json/parse-string keyword))))

(defn configure [auth-provider-url
                 client-id
                 client-secret
                 callback-uri]
  (if (and auth-provider-url client-id client-secret callback-uri)
    {:auth-provider-url auth-provider-url
     :client-id client-id
     :client-secret client-secret
     :callback-uri callback-uri}
    :invalid-configuration))

(defn stonecutter-sign-in [{:keys [stonecutter-config] :as request}]
  (authorisation-redirect-response stonecutter-config))

(defn stonecutter-callback [request]
  (let [auth-code (get-in request [:params :code])
        stonecutter-config (get-in request [:stonecutter-config])
        token-response (request-access-token! stonecutter-config auth-code)
        auth-provider-user-id (str "stonecutter-" (get-in token-response [:token :user-id]))]
    (-> (response/redirect (str utils/host-url "/sign-up"))
        (assoc-in [:session :auth-provider-user-id] auth-provider-user-id))))

(defn wrap-stonecutter-config [handler config invalid-configuration-handler]
  (case config
    :invalid-configuration invalid-configuration-handler
    (fn [request] (handler (assoc request :stonecutter-config config)))))

(defn invalid-configuration-handler [_]
  (response/redirect (utils/path-for :fe/error-configuration)))

(def stonecutter-routes
  ["/" {"stonecutter-sign-in" :sign-in
        "stonecutter-callback" :callback}])

(defn stonecutter-handlers [config]
  {:sign-in (wrap-stonecutter-config stonecutter-sign-in config invalid-configuration-handler)
   :callback (wrap-stonecutter-config stonecutter-callback config invalid-configuration-handler)})

(defn workflow [config]
  (bidi/make-handler stonecutter-routes (stonecutter-handlers config)))
