(ns objective8.front-end.workflows.okta
  (:require [objective8.utils :as utils]
            [ring.util.response :as response]
            [objective8.front-end.api.http :as http]
            [cheshire.core :as json]))

(def redirect-uri (str utils/host-url "/okta-callback"))

(defn okta-sign-in [{:keys [okta-config] :as request}]
  (let [client-id (:client-id okta-config)]
    (response/redirect (str "https://thoughtworks.oktapreview.com/oauth2/v1/authorize?client_id=" client-id
                            "&redirect_uri=" redirect-uri
                            "&scope=email&response_type=code"))))

(defn response->json [response]
  (json/parse-string (:body response) true))

(defn get-token-info [access-token]
  (let [response (http/get-request "https://thoughtworks.oktapreview.com/oauth2/v1/userinfo" {:headers {:oauth-token access-token}})]
    (response->json response)))

(defn check-token-info [request access-token]
  (let [token-info (get-token-info access-token)
        user-id (:sub token-info)
        updated-session (assoc (:session request) :auth-provider-user-id (str "okta-" user-id)
                                                  :auth-provider-user-email (:email token-info))]
    (into (response/redirect (str utils/host-url "/sign-up"))
          {:session updated-session})))

(defn get-access-token [{:keys [params okta-config] :as request}]
  (let [code (:code params)
        client-id (:client-id okta-config)
        client-secret (:client-secret okta-config)
        response (http/get-request "https://thoughtworks.oktapreview.com/oauth2/v1/token" {:query-params {:grant_type    "authorization_code"
                                                                                                          :client_id     client-id
                                                                                                          :redirect_uri  redirect-uri
                                                                                                          :client_secret client-secret
                                                                                                          :code          code}})]
    (response->json response)))

(defn check-access-token [request]
  (let [access-token-response (get-access-token request)]
    (check-token-info request (:access_token access-token-response))))

(defn okta-callback [request]
  (check-access-token request))