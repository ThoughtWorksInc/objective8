(ns user
  (:require [clojure.tools.namespace.repl :as tnr]
            [clojure.tools.logging :as log]
            [clojure.set :as s]
            [korma.db :as kdb]
            [objective8.core :as core]
            [objective8.config :as config]
            [objective8.integration.storage-helpers :as sh]
            [dev-helpers.stub-twitter-and-stonecutter :refer [stub-twitter-auth-config]]
            [dev-helpers.launch :refer [stop make-launcher-map]]))

; Don't try to load ./test and ./integration
(tnr/set-refresh-dirs "./src" "./dev")

(def local-spec (kdb/postgres {:db (config/get-var "DB_NAME" "objective8")
                              :user (config/get-var "DB_USER" "objective8")
                              :host (config/get-var "DB_HOST" "127.0.0.1")
                              :port (config/get-var "DB_PORT" 5432)}))

(defn load-test-sign-in [handler]
  (fn [request]
    (if (= "true" (get-in request [:headers "x-load-test-signed-in"]))
      (let [gatling-user-id (Integer/parseInt (get-in request [:headers "x-load-test-user-id"]))
            username (str "username-" gatling-user-id)
            session (merge (:session request)
                           {:cemerick.friend/identity {:current gatling-user-id
                                                       :authentications {gatling-user-id {:identity gatling-user-id
                                                                                          :roles #{:signed-in :writer-for-85826}
                                                                                          :username username}}}})]
        (handler (assoc request :session session)))
      (handler request))))

(def configs 
  {:default {:app-config core/app-config}
   :stub-twitter {:app-config (assoc core/app-config :authentication stub-twitter-auth-config)}
   :local-stub-twitter {:app-config (assoc core/app-config
                                           :authentication stub-twitter-auth-config
                                           :db-spec local-spec)}
   :profiling {:app-config (assoc core/app-config 
                                  :authentication stub-twitter-auth-config
                                  :profile-middleware load-test-sign-in)
               :profile? true}})

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

(defn seed-draft-comments []
  (let [draft (sh/store-a-draft)
        user (sh/store-a-user)]
    (loop [i 0]
      (when (< i 120)
        (sh/store-a-comment {:user user
                             :entity draft
                             :comment-text (str i " seed comment")})
        (recur (inc i))))))

(defn seed-comments []
  (let [objective (sh/store-an-objective)
        user (sh/store-a-user)]
    (loop [i 0]
      (when (< i 120)
        (sh/store-a-comment {:user user
                             :entity objective
                             :comment-text (str i " seed comment")})
        (recur (inc i))))))

(defn seed-questions []
  (let [objective (sh/store-an-objective)
        user (sh/store-a-user)]
    (loop [i 0]
      (when (< i 120)
        (sh/store-a-question {:user user
                              :objective objective})
        (recur (inc i))))))

(defn seed-objective [user]
  (let [objective (sh/store-an-objective {:user user :title (str "objective created by " (:username user))})
        writer (sh/store-a-writer {:user user :objective objective})
        objective-comments (doall (for [x (range 120)]
                                    (sh/store-a-comment {:entity objective :comment-text (str "objective comment - " x)})))
        draft (sh/store-a-draft {:objective objective :user user})
        draft-comments (doall (for [x (range 120)]
                                (sh/store-a-comment {:entity draft :comment-text (str "draft comment - " x)})))
        questions (doall (for [x (range 120)]
                           (sh/store-a-question {:user user :objective objective :question-text (str "question - " x)})))
        answers (doall (for [question questions
                             x (range 10)]
                         (sh/store-an-answer {:question question :answer-text (str "Answer to " (:question question) "is because of " x)})))
        section (sh/store-a-section {:draft draft :section-label "abcd1234"})
        annotations (doall (for [x (range 120)]
                             (:comment (sh/store-an-annotation {:section section
                                                                :annotation-text (str "annotation - " x)
                                                                :reason "general"}))))]
    (doseq [comment (flatten [(take 60 objective-comments)
                              (take 60 draft-comments)
                              (take 60 annotations)])]
      (sh/store-a-note {:note-on-entity comment
                        :writer writer
                        :note (str "note on " (:comment comment))}))))

(defn seed-data
  ([]
   (seed-answers)
   (seed-comments)
   (seed-draft-comments))

  ([user]
   (seed-objective user)))
