(ns objective8.questions
  (:require [cemerick.friend :as friend]
            [objective8.storage.storage :as storage]))

(defn request->question
  "Returns a map of a question with a user if all parts are in the request. Otherwise returns nil"
  [{{id :id} :route-params
    :keys [params]}]
  (assoc (select-keys params [:question])
          :created-by-id (get (friend/current-authentication) :username)
          :objective-id (Integer/parseInt id)))

(defn store-question! [question]
 (storage/pg-store! (assoc question :entity :question)))

(defn retrieve-question [question-id]
  (let [{result :result} (storage/pg-retrieve {:entity :question :_id question-id})]
  (dissoc (first result) :entity)))
