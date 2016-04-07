(ns objective8.front-end.workflows.okta
  (:require [objective8.utils :as utils]
            [ring.util.response :as response]
            [objective8.front-end.api.http :as http]
            [bidi.ring :refer [make-handler]]
            [cheshire.core :as json]))

(def redirect-uri (str utils/host-url "/okta-callback"))

(def okta-routes
  ["/" {"okta-sign-in"  :sign-in
        "okta-callback" :callback}])

(defn okta-sign-in [{:keys [okta-config] :as request}]
  (let [client-id (:client-id okta-config)
        auth-url (:auth-url okta-config)]
    (response/redirect (str auth-url "/oauth2/v1/authorize?client_id=" client-id
                            "&redirect_uri=" redirect-uri
                            "&scope=openid%20email&response_type=code"))))

(defn response->json [response]
  (json/parse-string (:body response) true))

(defn get-user-info [access-token auth-url]
  (http/get-request (str auth-url "/oauth2/v1/userinfo") {:oauth-token access-token}))

(defn user-info-valid? [user-info user-info-body auth-url]
  (let [expiry-time (:exp user-info-body)
        issuer-url (:iss user-info-body)]
    (and (= (:status user-info) 200)
         (> expiry-time (utils/unix-current-time))
         (= issuer-url auth-url))))

(defn check-token-info [request access-token]
  (let [auth-url (get-in request [:okta-config :auth-url])
        user-info (get-user-info access-token auth-url)
        user-info-body (response->json user-info)
        user-id (:sub user-info-body)
        updated-session (assoc (:session request) :auth-provider-user-id (str "okta-" user-id)
                                                  :auth-provider-user-email (:email user-info-body))]

    (if (user-info-valid? user-info user-info-body auth-url)
      (into (response/redirect (str utils/host-url "/sign-up"))
            {:session updated-session})
      (response/redirect (str utils/host-url "/error/log-in")))))

(defn get-access-token [{:keys [params okta-config] :as request}]
  (let [code (:code params)
        client-id (:client-id okta-config)
        client-secret (:client-secret okta-config)
        auth-url (:auth-url okta-config)
        response (http/get-request (str auth-url "/oauth2/v1/token") {:query-params {:grant_type    "authorization_code"
                                                                                     :client_id     client-id
                                                                                     :redirect_uri  redirect-uri
                                                                                     :client_secret client-secret
                                                                                     :code          code}})]
    (response->json response)))

(defn check-access-token [request]
  (let [access-token-response (get-access-token request)]
    (if (:error access-token-response)
      (response/redirect (str utils/host-url "/error/log-in"))
      (check-token-info request (:access_token access-token-response)))))

(defn okta-callback [{:keys [params] :as request}]
  (if (:error params)
    (response/redirect (str utils/host-url "/error/log-in"))
    (check-access-token request)))

(defn wrap-handler [handler okta-config]
  (fn [request] (handler (assoc request :okta-config okta-config))))

(defn okta-handlers [okta-config]
  {:sign-in  (wrap-handler okta-sign-in okta-config)
   :callback (wrap-handler okta-callback okta-config)})

(defn okta-workflow [okta-config]
  (make-handler okta-routes (okta-handlers okta-config)))