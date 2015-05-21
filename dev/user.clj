(ns user
  (:require [clojure.tools.namespace.repl :as tnr]
            [clojure.tools.logging :as log]
            [org.httpkit.server :as server]
            [clojure.set :as s]
            [korma.db :as kdb]
            [objective8.core :as core]
            [objective8.config :as config]
            [objective8.integration.storage-helpers :as sh]
            [objective8.back-end.storage.database :as db]
            [objective8.utils :as utils]
            [dev-helpers.stub-twitter :refer [stub-twitter-auth-config]]
            [dev-helpers.launch :refer [stop make-launcher-map]]))

; Don't try to load ./test and ./integration
(tnr/set-refresh-dirs "./src" "./dev")

(def local-spec (kdb/postgres {:db (config/get-var "DB_NAME" "objective8")
                              :user (config/get-var "DB_USER" "objective8")
                              :host (config/get-var "DB_HOST" "127.0.0.1")
                              :port (config/get-var "DB_PORT" 5432)}))

(def configs 
  {:default core/app-config
   :stub-twitter (assoc core/app-config :authentication stub-twitter-auth-config)
   :local-stub-twitter (assoc core/app-config
                              :authentication stub-twitter-auth-config
                              :db-spec local-spec)})

(def launchers (make-launcher-map configs))

(defn reset [config-key]
  (let [post-reset-hook (get launchers config-key (:default launchers))]
    (stop)
    (tnr/refresh :after post-reset-hook)
    (log/info "Reset")
    config-key))

;; Useful helpers
(defn print-and-pass-through
  ([v] (print-and-pass-through v ""))
  
  ([v tag]
   (log/info (str tag v))
   v))

(defn diff-maps [map-1 map-2 & tags]
  (let [map-1-keys (set (keys map-1))
        map-2-keys (set (keys map-2))
        common-keys (vec (s/intersection map-1-keys map-2-keys))
        make-delta-keyword #(keyword (str "in-" (first %) "-not-" (second %)))
        in-1-but-not-2-key (make-delta-keyword (if tags tags [1 2]))
        in-2-but-not-1-key (make-delta-keyword (if tags (reverse tags) [2 1]))]

    {:difference-on-common-keys (->> (map vector common-keys (map (juxt map-1 map-2) common-keys))
                       (filter (comp (partial apply not=) (juxt first second) second))
                       (into {}))
     in-1-but-not-2-key (select-keys map-1 (s/difference map-1-keys map-2-keys))
     in-2-but-not-1-key (select-keys map-2 (s/difference map-2-keys map-1-keys))}))

;; Seed Data

(defn seed-answers []
  (let [question (sh/store-a-question)
        user (sh/store-a-user)]
    (loop [i 0]
      (when (< i 120)
        (sh/store-an-answer {:question question
                             :user user
                             :answer-text (str i " seed answer")})  
        (recur (inc i))))))

(defn seed-comments []
  (let [objective (sh/store-an-open-objective)
        user (sh/store-a-user)]
    (loop [i 0]
      (when (< i 120)
        (sh/store-a-comment {:user user
                             :entity objective
                             :comment-text (str i " seed comment")})
        (recur (inc i))))))

(defn seed-questions []
  (let [objective (sh/store-an-open-objective)
        user (sh/store-a-user)]
    (loop [i 0]
      (when (< i 120)
        (sh/store-a-question {:user user
                             :objective objective})
        (recur (inc i))))))

(defn seed-data []
  (seed-answers)
  (seed-comments))
