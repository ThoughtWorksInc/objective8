(ns objective8.utils
  (:require [clj-time.format :as time-format]
            [clj-time.core :as time-core]
            [clojure.string :as s]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [bidi.bidi :as bidi]
            [endophile.hiccup :as eh]
            [hiccup.core :as hiccup]
            [hickory.core :as hickory]
            [clojure.tools.logging :as log]
            [objective8.routes :as routes]
            [objective8.config :as config]
            [objective8.front-end.permissions :as permissions])
  (:import  [org.pegdown PegDownProcessor Extensions]))

(def host-url
  (str (config/get-var "HTTPS" "http://") (config/get-var "BASE_URI" "localhost:8080")))

;;Map manipulation
(defn select-all-or-nothing
  "If m contains all of the keys in required-keys, then returns a
  submap containing just the required keys, otherwise returns nil"
  [m required-keys]
  (let [present-keys (set (keys m))]
    (when (every? present-keys required-keys)
      (select-keys m required-keys))))

(defn ressoc [m old-key new-key]
  (-> m
      (dissoc old-key)
      (assoc new-key (old-key m))))

(defn update-in-self [m key-route update-fn]
  (assoc-in m key-route (update-fn m)))

(defn transform-map-keys [m transformation]
  (let [ks (keys m) vs (vals m)]
    (zipmap (map transformation ks) vs)))

;;Bidi currently doesn't currently work with java.lang.Integer
(extend-protocol bidi/ParameterEncoding
  java.lang.Integer
  (bidi/encode-parameter [s] s))

(defn path-for [& args]
  (str host-url (apply bidi/path-for routes/routes args)))

(defn local-path-for [& args]
  (apply bidi/path-for routes/routes args))

(defn generate-random-uuid []
  (str (java.util.UUID/randomUUID)))

;;TIME FORMATTING

(defn current-time []
  (time-core/now))

(defn days-until [date-time]
  (if (time-core/after? (current-time) date-time)
    0
    (time-core/in-days (time-core/interval (current-time) date-time))))

(defn string->date-time [date-string]
  (time-format/parse (time-format/formatters :year-month-day) date-string))

(defn date-time->iso-time-string [date-time]
  (str date-time))

(defn time-string->date-time [time-string]
  (time-format/parse (time-format/formatters :date-time) time-string))

(def pretty-date (time-format/formatter "dd-MM-yyyy"))

(def pretty-date-time (time-format/formatter "dd-MM-yyyy HH:mm"))

(defn date-time->pretty-date [date-time]
  (time-format/unparse pretty-date date-time))

(defn iso-time-string->pretty-date [iso-time-string]
  (time-format/unparse pretty-date (time-string->date-time iso-time-string)))

(defn iso-time-string->pretty-time [iso-time-string]
  (time-format/unparse pretty-date-time (time-string->date-time iso-time-string)))

(defn date-time->date-time-plus-30-days [date-time]
  (time-core/plus date-time (time-core/days 30)))

(defn- regex-checker
  [fragment-regex]
    (fn [fragment] (when fragment (re-matches fragment-regex fragment))))

(defn safen-path [target]
  (or ((regex-checker #"/learn-more") target)
      ((regex-checker #"/") target)
      ((regex-checker #"/objectives") target)
      ((regex-checker #"/objectives/\d+") target)
      ((regex-checker #"/objectives/\d+/comments") target)
      ((regex-checker #"/objectives/\d+/add-question") target)
      ((regex-checker #"/objectives/\d+/questions") target)
      ((regex-checker #"/objectives/\d+/questions/\d+") target)
      ((regex-checker #"/objectives/\d+/drafts") target)
      ((regex-checker #"/objectives/\d+/drafts/\d+") target)
      ((regex-checker #"/objectives/\d+/drafts/\d+/comments") target)
      ((regex-checker #"/objectives/\d+/drafts/latest") target)
      ((regex-checker #"/objectives/\d+/drafts/latest/comments") target)
      ((regex-checker #"/objectives/\d+/drafts/\d+/sections/[0-9a-f]{8}") target)
      ((regex-checker #"/objectives/\d+/drafts/add-draft") target)
      ((regex-checker #"/objectives/\d+/invite-writer") target)
      ((regex-checker #"/objectives/\d+/writers") target)
      ((regex-checker #"/objectives/\d+/writers/invitation") target)
      ((regex-checker #"/objectives/\d+/writer-invitations/\d+") target)
      ((regex-checker #"/objectives/\d+/dashboard/questions") target)
      ((regex-checker #"/objectives/\d+/dashboard/comments") target)
      ((regex-checker #"/objectives/\d+/dashboard/annotations") target)))

(def valid-query-string-regex
  (let [query-term "(?:[a-zA-Z-]+=[a-zA-Z0-9-|/%]*)"]
    (re-pattern (str query-term "(?:&" query-term ")*"))))

(defn safen-query [query] 
  (when query
    ((regex-checker valid-query-string-regex) query)))

(defn safen-fragment [fragment]
  (when fragment
    (or ((regex-checker #"comments") fragment)
        ((regex-checker #"comment-\d+") fragment)
        ((regex-checker #"add-comment-form") fragment)
        ((regex-checker #"questions") fragment)
        ((regex-checker #"answer-\d+") fragment))))

(defn url->url-map [url]
   (let [[url-without-fragment fragment] (s/split url #"#" 2)
         [path query] (s/split url-without-fragment #"\?" 2)]
     {:path path
      :query query
      :fragment fragment}))

(defn safen-url [target]
  (let [{:keys [path query fragment] :as url-map} (url->url-map target)
        safe-path (safen-path path)
        safe-query (safen-query query)
        safe-fragment (safen-fragment fragment)
        safe-url (cond-> safe-path
                   safe-query (str "?" safe-query)
                   safe-fragment (str "#" safe-fragment))]
    (when-not (= target safe-url)
      (log/warn (str "Unsafe-url safened: \"" target "\" became \"" safe-url "\"")))
    safe-url))

(defn referer-url [{:keys [uri query-string] :as ring-request}]
  (str uri (when query-string
             (str "?" query-string))))

(defn anti-forgery-hook 
  "Hook enables CSRF when config variable set. Can be disabled for tests"
  [handler]
  (let [handler-with-anti-forgery (wrap-anti-forgery handler)]
    (fn [request] (if config/enable-csrf
                    (handler-with-anti-forgery request)
                    (handler request)))))


;; HICCUP & MARKDOWN

(defn sanitise-hiccup [hiccup]
  (->> (remove nil? hiccup)
       (filter sequential?)
       (apply list)))

(defn- parse-markdown 
  "Extends endophile.core's 'md' method to include SUPPRESS_ALL_HTML"
  [md]
  (.parseMarkdown
    (PegDownProcessor.  (int
                          (bit-or
                            Extensions/AUTOLINKS
                            Extensions/SUPPRESS_ALL_HTML
                            Extensions/FENCED_CODE_BLOCKS)))
    (char-array md)))

(defn markdown->hiccup [md]
  (->> md
       parse-markdown
       eh/to-hiccup
       sanitise-hiccup))

(defn hiccup->html [hcp]
  (-> (sanitise-hiccup hcp)
      hiccup/html))

(defn html->hiccup [html]
  (map hickory/as-hiccup (hickory/parse-fragment html)))

(defn split-hiccup-element 
  "Returns a map with :element-without-content and :content
   :element-without-content is either:
   - just the html tag (string or key) (e.g. [:h1])
   - the html tag plus a map of attributes or nil (e.g. [:h1 nil])
   :content is a vector containing strings and/or child hiccup html elements"
  [element]
  (if (= (count element) 1)
    {:element-without-content element
     :content []}
    (let [attrs (second element)]
      (if (or (map? attrs) (nil? attrs))
        {:element-without-content (subvec element 0 2)
         :content (subvec element 2)}
        {:element-without-content (subvec element 0 1)
         :content (subvec element 1)}))))
