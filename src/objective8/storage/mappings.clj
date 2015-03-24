(ns objective8.storage.mappings
  (:refer-clojure :exclude [comment])
  (:require [korma.core :as korma]
            [cheshire.core :as json]
            [clj-time [format :as tf] [coerce :as tc]]
            [objective8.utils :as utils])
  (:import [org.postgresql.util PGobject]))

(defn sql-time->iso-time-string [sql-time]
  (utils/date-time->iso-time-string (tc/from-sql-time sql-time)))

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

(defn map->objective
  "Converts a clojure map into a json-typed objective for the database"
  [{:keys [created-by-id end-date global-id] :as objective}]
  (if (and created-by-id end-date global-id)
    {:created_by_id created-by-id
     :global_id global-id
     :end_date (tc/to-timestamp end-date)
     :objective (map->json-type objective)}
      (throw (Exception. "Could not transform map to objective"))))

(defn map->comment
  "Converts a clojure map into a json-typed comment for the database"
  [{:keys [created-by-id objective-id comment-on-id] :as comment}]
  (if (and created-by-id objective-id comment-on-id)
    {:created_by_id created-by-id
     :objective_id objective-id
     :comment_on_id comment-on-id
     :comment (map->json-type comment)}
    (throw (Exception. "Could not transform map to comment"))))

(defn map->user
  "Converts a clojure map into a json-typed user for the database"
  [{:keys [twitter-id username] :as user}]
  (if (and twitter-id username) 
    {:twitter_id twitter-id
     :username username
     :user_data (map->json-type user)}
    (throw (Exception. "Could not transform map to user"))))

(defn map->up-down-vote
  "Prepares a clojure map for storage as an up-down-vote"
  [{:keys [global-id created-by-id vote-type] :as up-down-vote}]
  (if (and global-id created-by-id (#{:up :down} vote-type))
    {:global_id global-id
     :created_by_id created-by-id
     :vote ({:up 1 :down -1} vote-type)}
    (throw (ex-info "Could not transform map to up-down-vote" {:data up-down-vote}))))

(defn map->question
  "Converts a clojure map into a json-typed question for the database"
  [{:keys [created-by-id objective-id] :as question}]
  (if (and created-by-id objective-id)
    {:created_by_id created-by-id
     :objective_id objective-id
     :question (map->json-type question)}
    (throw (Exception. "Could not transform map to question"))))

(defn map->answer 
  "Converts a clojure map into a json-typed answer for the database"
  [{:keys [created-by-id question-id global-id objective-id] :as answer}]
  (if (and created-by-id question-id global-id objective-id)
    {:created_by_id created-by-id
     :question_id question-id
     :objective_id objective-id
     :global_id global-id
     :answer (map->json-type answer)}
    (throw (ex-info "Could not transform map to answer" {:data answer}))))

(defn map->invitation
 "Converts a clojure map into a json-typed invitation for the database" 
  [{:keys [invited-by-id objective-id uuid status] :as invitation}]
  (if (and invited-by-id objective-id uuid status)
    {:invited_by_id invited-by-id
     :objective_id objective-id
     :uuid uuid
     :status (string->postgres-type "invitation_status" status)
     :invitation (map->json-type invitation)}
    (throw (Exception. "Could not transform map to invitation"))))

(defn map->candidate
 "Converts a clojure map into a json-typed candidate for the database"
  [{:keys [user-id objective-id invitation-id] :as candidate}]
  (if (and user-id objective-id invitation-id)
    {:user_id user-id
     :objective_id objective-id
     :invitation_id invitation-id
     :candidate (map->json-type candidate)}
    (throw (Exception. "Could not transform map to candidate"))))

(defn map->draft
  "Converts a clojure map into a json-typed draft for the database"
  [{:keys [submitter-id objective-id global-id] :as draft}]
  (if (and submitter-id objective-id global-id)
    {:submitter_id submitter-id
     :objective_id objective-id
     :global_id global-id
     :draft (map->json-type draft)}
    (throw (Exception. "Could not transform map to draft"))))

(defn map->bearer-token
 "Converts a clojure map into a json-typed bearer-token for the database"
 [{:keys [bearer-name] :as bearer-token}]
  (if bearer-name
    {:bearer_name bearer-name
     :token_details (map->json-type bearer-token)}
    (throw (Exception. "Could not transform map to bearer-token"))))

(defn unmap [data-key]
  (fn [m] (assoc (json-type->map (data-key m))
                 :_id (:_id m)
                 :_created_at (sql-time->iso-time-string (:_created_at m)))))

(defn unmap-with-username [data-key]
  (fn [m] (assoc (json-type->map (data-key m))
                 :_id (:_id m)
                 :_created_at (sql-time->iso-time-string (:_created_at m))
                 :username (:username m))))

(defn unmap-with-sql-time [data-key]
  (fn [m] (assoc (json-type->map (data-key m))
                 :_id (:_id m)
                 :_created_at (sql-time->iso-time-string (:_created_at m))
                 :_created_at_sql_time (:_created_at m)
                 :username (:username m))))

(declare objective user comment question answer invitation candidate bearer-token up-down-vote)

(korma/defentity global-identifier
  (korma/pk :_id)
  (korma/table :objective8.global_identifiers))

(korma/defentity objective
  (korma/pk :_id)
  (korma/table :objective8.objectives)
  (korma/belongs-to user {:fk :created_by_id})
  (korma/prepare map->objective)
  (korma/transform (unmap-with-username :objective)))

(korma/defentity user
  (korma/pk :_id)
  (korma/table :objective8.users)
  (korma/prepare map->user)
  (korma/transform (unmap :user_data)))

(korma/defentity comment
  (korma/pk :_id)
  (korma/table :objective8.comments)
  (korma/belongs-to user {:fk :created_by_id})
  (korma/prepare map->comment)
  (korma/transform (unmap-with-username :comment)))

(korma/defentity question
  (korma/pk :_id)
  (korma/table :objective8.questions)
  (korma/belongs-to user {:fk :created_by_id})
  (korma/prepare map->question)
  (korma/transform (unmap-with-username :question)))

(korma/defentity answer
  (korma/pk :_id)
  (korma/table :objective8.answers)
  (korma/belongs-to user {:fk :created_by_id})
  (korma/prepare map->answer)
  (korma/transform (unmap-with-username :answer)))

(korma/defentity invitation
  (korma/pk :_id)
  (korma/table :objective8.invitations)
  (korma/prepare map->invitation)
  (korma/transform (unmap :invitation)))

(korma/defentity candidate
  (korma/pk :_id)
  (korma/table :objective8.candidates)
  (korma/prepare map->candidate)
  (korma/transform (unmap :candidate)))

(defn ressoc [m old-key new-key]
  (-> m
      (dissoc old-key)
      (assoc new-key (old-key m))))

(defn unmap-up-down-vote [{vote :vote :as m}]
  (-> m
      (ressoc :global_id :global-id)
      (ressoc :created_by_id :created-by-id)
      (dissoc :vote)
      (assoc :vote-type ({1 :up -1 :down} vote))))

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
  (korma/transform (unmap-with-sql-time :draft)))

(korma/defentity bearer-token
  (korma/pk :_id)
  (korma/table :objective8.bearer_tokens)
  (korma/prepare map->bearer-token)
  (korma/transform (unmap :token_details)))

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
