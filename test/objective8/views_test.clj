(ns objective8.views_test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [objective8.views :as views]
            ))

(def test-view identity)

(defn fake-translation-function [k]
  (get {:test/doc-title "This is a title"
        :test/doc-description "This is a description"}
       k
       "FAIL!"))

; Note - this request has been editied, it's not a a full session
(def ring-request {:jvm-locales [:en :en-US],
                   :locale :en,
                   :cookies {"ring-session" {:value "435f99e9-f21d-471b-b807-d0b53358fcce"}},
                   :remote-addr "192.168.50.1",
                   :jvm-locale :en,
                   :t' fake-translation-function,
                   :params {},
                   :flash nil,
                   :route-params nil,
                   :headers {"Headers" "go here"},
                   :server-port 8080,
                   :content-length 0,
                   :locales [:en :en-US],
                   :form-params {},
                   :websocket? false,
                   :session/key "435f99e9-f21d-471b-b807-d0b53358fcce",
                   :query-params {},
                   :content-type nil,
                   :character-encoding "utf8",
                   :t "This is the translations function we don't use at the moment",
                   :uri "/objectives",
                   :server-name "192.168.50.50",
                   :query-string nil,
                   :body nil,
                   :scheme :http,
                   :cemerick.friend/auth-config {:allow-anon? true,
                                                 :default-landing-uri "/",
                                                 :login-uri "/sign-in",
                                                 :credential-fn "credentials function",
                                                 :workflows ["Bunch of handler functions"]},
                   :request-method :get,
                   :session {:cemerick.friend/identity {:current 155,
                                                        :authentications {155 {:identity 155, :username 155, :roles #{:signed-in}}}},
                             :ring.middleware.anti-forgery/anti-forgery-token "V/ASBxBlK3hM2RGvK5IbXcFvlUrnyNxYFzI6Dwfy8VNYbpAeNVOXFnm7EfP0bXsIg5dsOVS/q0d4Zrn/",
                             :twitter-screen-name "twitter name if signed in",
                             :twitter-id "twitter-NUMBERS"},
                   :tconfig {:dictionary {:es {}, :en {}}, :dev-mode? false, :fallback-locale :en, :log-missing-translations-function "log missing translations function"}})

(fact "calls the wrapped function with a view-context based on a ring request"
      (let [view-fn (views/view test-view)]
        (view-fn "index" {:request "request"}) => (contains {:ring-request {:request "request"}}) 

        (fact "pulls out translation information"
              (view-fn "test" ring-request) => (contains {:translations fn?}))

        (fact "pulls out common page information from the translations"
              (view-fn "test" ring-request) => (contains {:doc {:title "This is a title"
                                                                :description "This is a description"}}))

        (fact "pulls out user information if the user is authenticated with friend")
              (view-fn "test" ring-request) => (contains {:user {:display-name "Wibble"}})
              (provided
                (friend/current-authentication ring-request) => {:display-name "Wibble"})
        
        (fact "user is nil if there is no friend authentication"
              (view-fn "test" ring-request) => (contains {:user nil}) 
              (provided
                (friend/current-authentication ring-request) => nil))

        (fact "data is passed to the view"
              (view-fn "test" ring-request :data "yay") => (contains {:data {:data "yay"}}))))

