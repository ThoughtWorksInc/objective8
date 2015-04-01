(ns objective8.storage.mappings
  (:refer-clojure :exclude [comment])
  (:require [korma.core :as korma]
            [cheshire.core :as json]
            [clj-time [format :as tf] [coerce :as tc]]
            [objective8.utils :as utils])
  (:import [org.postgresql.util PGobject]))

(defn sql-time->iso-time-string [sql-time]
  (utils/date-time->iso-time-string (tc/from-sql-time sql-time)))

(defn iso-date-time->sql-time [date-time]
  (tc/to-sql-time date-time))

(defn string->postgres-type
  "Convert a string to a Postgres object with the given type."
  [type value]
  (doto (PGobject.)
    (.setType type)
    (.setValue value)))

(defn postgres-type->string
  "Convert a postgres object to its string representation"
  [pgobject]
  (.getValue pgobject))

(defn map->json-type
  "Convert a clojure map to a Postgres JSON type"
  [m]
  (string->postgres-type "json" (json/generate-string m)))

(defn json-type->map
  "Convert a Postgres JSON type to a clojure map"
  [pgobject]
  (json/parse-string (postgres-type->string pgobject) true))

(defn -to_
  "Replaces hyphens in keys with underscores"
  [m]
  (let [ks (keys m) vs (vals m)]
    (zipmap (map (fn [k] (-> (name k)
                             (clojure.string/replace #"-" "_")
                             keyword)) ks)
            vs)))

(defn- apply-transformation [m [key transform]]
  (update-in m [key] transform))

(defn- apply-transformations [m transformations]
  (reduce apply-transformation m transformations))

(defn db-insertion-mapper
  "Generates a function that prepares a map for insertion into the database

  entity-label: A string used to identify the entity type
  json-key: The database column that will contain any unstructured data; set this to nil if not required
  column-keys: A vector of the keys in the clojure map that correspond to columns in the database
  transformations: A map indicating any additional transformations that need to be performed on data fields before insertion into the database.

  Example:
  (def map->some-entity (db-insertion-mapper \"some-entity\" :the-json [:a-key :b-key] {:a-key inc}))

  (some-entity {:a-key 1 :b-key 2 :other \"data\"})
  => {:a_key 2
      :b_key 2
      :the_json <PG Json object {\"other\":\"data\"}>}"

  ([entity-label json-key column-keys]
   (db-insertion-mapper entity-label json-key column-keys {}))

  ([entity-label json-key column-keys transformations]
   (fn [m]
     (if-let [column-map (utils/select-all-or-nothing m column-keys)]
       (let [insert-json-if-required (if json-key
                                       #(assoc % json-key (map->json-type (apply dissoc m column-keys)))
                                       identity)]
         (-> column-map
             insert-json-if-required
             (apply-transformations transformations)
             -to_))
       (throw (ex-info (str "Could not transform map to " entity-label) {:data m}))))))

(def map->objective
  (db-insertion-mapper "objective"
                       :objective
                       [:created-by-id :global-id :status :end-date]
                       {:status (partial string->postgres-type "objective_status")
                        :end-date tc/to-timestamp}))

(def map->comment
  (db-insertion-mapper "comment"
                       :comment
                       [:global-id :created-by-id :objective-id :comment-on-id]))

(def map->user
  (db-insertion-mapper "user"
                       :user-data
                       [:twitter-id :username]))

(defn map->up-down-vote
  "Prepares a clojure map for storage as an up-down-vote"
  [{:keys [global-id created-by-id vote-type] :as up-down-vote}]
  (if (and global-id created-by-id (#{:up :down} vote-type))
    {:global_id global-id
     :created_by_id created-by-id
     :vote ({:up 1 :down -1} vote-type)}
    (throw (ex-info "Could not transform map to up-down-vote" {:data up-down-vote}))))

(def map->question
  (db-insertion-mapper "question"
                       :question
                       [:created-by-id :objective-id]))

(def map->answer
  (db-insertion-mapper "answer"
                       :answer
                       [:created-by-id :question-id :objective-id :global-id]))

(def map->invitation
  (db-insertion-mapper "invitation"
                       :invitation
                       [:invited-by-id :objective-id :uuid :status]
                       {:status (partial string->postgres-type "invitation_status")}))

(def map->candidate
  (db-insertion-mapper "candidate"
                       :candidate
                       [:user-id :objective-id :invitation-id]))

(def map->draft
  (db-insertion-mapper "draft"
                       :draft
                       [:submitter-id :objective-id :global-id]))

(def map->bearer-token
  (db-insertion-mapper "bearer-token"
                       :token-details
                       [:bearer-name]))

(defn unmap [data-key]
  (fn [m] (-> (json-type->map (data-key m))
              (assoc :_id (:_id m) :_created_at (sql-time->iso-time-string (:_created_at m)))
              (update-in [:entity] keyword))))

(defn with-username-if-present [unmap-fn]
  (fn [m] (let [m' (unmap-fn m)]
            (if (contains? m :username)
              (assoc m' :username (:username m))
              m'))))

(defn with-status [unmap-fn]
  (fn [m] (-> (unmap-fn m)
              (assoc :status (:status m)))))

(defn with-column
  ([unmap-fn new-key old-key] (with-column unmap-fn new-key old-key identity))

  ([unmap-fn new-key old-key transformation]
   (let [transformation (if transformation transformation identity)])
   (fn [m]
     (-> (unmap-fn m)
         (assoc new-key (transformation (old-key m)))))))

(defn with-sql-time [unmap-fn]
  (fn [m] (-> (unmap-fn m)
              (assoc :_created_at_sql_time (:_created_at m)))))

(defn with-global-id [unmap-fn]
  (fn [m] (-> (unmap-fn m)
              (assoc :global-id (:global_id m)))))

(defn without-key [unmap-fn key]
  (fn [m] (-> (unmap-fn m)
              (dissoc key))))

(defn unmap-up-down-vote [{vote :vote :as m}]
  (-> m
      (utils/ressoc :global_id :global-id)
      (utils/ressoc :created_by_id :created-by-id)
      (dissoc :vote)
      (assoc :vote-type ({1 :up -1 :down} vote))
      (assoc :entity :up-down-vote)))

(declare objective user comment question answer invitation candidate bearer-token up-down-vote)

(korma/defentity global-identifier
  (korma/pk :_id)
  (korma/table :objective8.global_identifiers))

(korma/defentity objective
  (korma/pk :_id)
  (korma/table :objective8.objectives)
  (korma/belongs-to user {:fk :created_by_id})
  (korma/prepare map->objective)
  (korma/transform (-> (unmap :objective)
                       (with-column :global-id :global_id)
                       (with-column :created-by-id :created_by_id)
                       (with-column :status :status)
                       (with-column :end-date :end_date sql-time->iso-time-string)
                       with-status
                       with-username-if-present)))

(korma/defentity user
  (korma/pk :_id)
  (korma/table :objective8.users)
  (korma/prepare map->user)
  (korma/transform (-> (unmap :user_data)
                       (with-column :twitter-id :twitter_id)
                       (with-column :username :username))))

(korma/defentity comment
  (korma/pk :_id)
  (korma/table :objective8.comments)
  (korma/belongs-to user {:fk :created_by_id})
  (korma/prepare map->comment)
  (korma/transform (-> (unmap :comment)
                       (with-column :comment-on-id :comment_on_id)
                       (with-column :created-by-id :created_by_id)
                       with-username-if-present
                       with-global-id
                       (without-key :objective-id))))

(korma/defentity question
  (korma/pk :_id)
  (korma/table :objective8.questions)
  (korma/belongs-to user {:fk :created_by_id})
  (korma/prepare map->question)
  (korma/transform (-> (unmap :question)
                       with-username-if-present
                       (with-column :created-by-id :created_by_id)
                       (with-column :objective-id :objective_id))))

(korma/defentity answer
  (korma/pk :_id)
  (korma/table :objective8.answers)
  (korma/belongs-to user {:fk :created_by_id})
  (korma/prepare map->answer)
  (korma/transform (-> (unmap :answer)
                       with-username-if-present
                       (with-column :created-by-id :created_by_id)
                       (with-column :objective-id :objective_id)
                       (with-column :question-id :question_id)
                       (with-column :global-id :global_id))))

(korma/defentity invitation
  (korma/pk :_id)
  (korma/table :objective8.invitations)
  (korma/prepare map->invitation)
  (korma/transform (-> (unmap :invitation)
                       (with-column :objective-id :objective_id)
                       (with-column :invited-by-id :invited_by_id)
                       (with-column :uuid :uuid)
                       (with-column :status :status))))

(korma/defentity candidate
  (korma/pk :_id)
  (korma/table :objective8.candidates)
  (korma/prepare map->candidate)
  (korma/transform (-> (unmap :candidate)
                       (with-column :objective-id :objective_id)
                       (with-column :user-id :user_id)
                       (with-column :invitation-id :invitation_id))))

(korma/defentity up-down-vote
  (korma/pk :_id)
  (korma/table :objective8.up_down_votes)
  (korma/prepare map->up-down-vote)
  (korma/transform unmap-up-down-vote))

(korma/defentity draft
  (korma/pk :_id)
  (korma/table :objective8.drafts)
  (korma/belongs-to user {:fk :submitter_id})
  (korma/prepare map->draft)
  (korma/transform (-> (unmap :draft)
                       (with-column :objective-id :objective_id)
                       (with-column :submitter-id :submitter_id)
                       with-username-if-present
                       with-sql-time
                       with-global-id)))

(korma/defentity bearer-token
  (korma/pk :_id)
  (korma/table :objective8.bearer_tokens)
  (korma/prepare map->bearer-token)
  (korma/transform (-> (unmap :token_details)
                       (with-column :bearer-name :bearer_name))))

(def entities {:objective objective
               :user      user
               :comment   comment
               :question  question
               :answer    answer
               :invitation invitation
               :candidate candidate
               :up-down-vote up-down-vote
               :draft draft
               :bearer-token bearer-token
               :global-identifier global-identifier})

(defn get-mapping
  "Returns a korma entity for a map"
  [{:keys [entity]}]
  (if-let [_entity (get entities entity)]
    _entity
    (throw (ex-info "No entity mapping for " {:entity entity}))))
