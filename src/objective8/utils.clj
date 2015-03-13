(ns objective8.utils
  (:require [clj-time.format :as time-format]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [bidi.bidi :as bidi]
            [cemerick.friend :as friend]
            [objective8.routes :as routes]
            [objective8.config :as config]))

(def host-url
  (str (config/get-var "HTTPS" "http://") (config/get-var "BASE_URI" "localhost:8080")))

;;Bidi currently doesn't currently work with java.lang.Integer
(extend-protocol bidi/ParameterEncoding
  java.lang.Integer
  (bidi/encode-parameter [s] s))

(defn path-for [& args] 
  (str host-url (apply bidi/path-for routes/routes args)))

(defn generate-random-uuid []
  (str (java.util.UUID/randomUUID)))

;;AUTHORISATION HELPERS

(defn writer-for [objective-id]
  (keyword (str "writer-for-" objective-id)))

(defn writer-for? [user objective-id]
  (contains? (:roles user) (writer-for objective-id)))

(defn add-authorisation-role
  "If the session in the request-or-response is already authenticated,
  then adds a new-role to the list of authorised roles, otherwise
  returns the request-or-response."
  [request-or-response new-role]
  (if-let [new-authentication (some-> (friend/current-authentication request-or-response)
                                      (update-in [:roles] conj new-role))]
    (friend/merge-authentication request-or-response new-authentication)
    request-or-response))

;;TIME FORMATTING

(defn string->date-time [date-string]
  (time-format/parse (time-format/formatters :year-month-day) date-string))

(defn date-time->iso-time-string [date-time]
  (str date-time))

(defn time-string->date-time [time-string]
  (time-format/parse (time-format/formatters :date-time) time-string))

(def pretty-date (time-format/formatter "dd-MM-yyyy"))

(def pretty-date-time (time-format/formatter "dd-MM-yyyy HH:mm"))

(defn time-string->pretty-date [time-string]
  (time-format/unparse pretty-date (time-string->date-time time-string)))

(defn date-time->pretty-date [date-time]
  (time-format/unparse pretty-date date-time))

(defn iso-time-string->pretty-time [iso-time-string]
  (time-format/unparse pretty-date-time (time-string->date-time iso-time-string)))

(defn- regex-checker
  [fragment-regex]
    (fn [fragment] (when fragment (re-matches fragment-regex fragment))))

(defn safen-url [target]
  (or ((regex-checker #"/objectives/\d+") target)
      ((regex-checker #"/objectives/\d+/questions") target)
      ((regex-checker #"/objectives/\d+/writers") target)
      ((regex-checker #"/objectives/\d+/writers/invitation") target)
      ((regex-checker #"/objectives/\d+/candidate-writers") target)
      ((regex-checker #"/objectives/\d+/questions/\d+") target)
      ((regex-checker #"/objectives/\d+/writer-invitations/\d+") target)))

;;DISABLE CSRF for tests

(defn anti-forgery-hook [handler]
  (let [handler-with-anti-forgery (wrap-anti-forgery handler)]
    (fn [request] (if config/enable-csrf
                    (handler-with-anti-forgery request)
                    (handler request)))))
