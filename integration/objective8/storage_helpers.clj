(ns objective8.storage-helpers
  (:require [objective8.storage.storage :as storage]))


(def username-index (atom 0))

(defn generate-unique-username []
  (str "username-" (swap! username-index inc)))

(defn store-a-user [] (storage/pg-store! {:entity :user
                                          :twitter-id "twitter-TWITTER_ID"
                                          :username (generate-unique-username)}))

(defn store-an-objective []
  (let [{user-id :_id} (store-a-user)]
    (storage/pg-store! {:entity :objective
                        :created-by-id user-id
                        :end-date "2015-01-01"})))

(defn store-an-invitation []
  (let [{invited-by-id :_id} (store-a-user)
        {objective-id :_id} (store-an-objective)]
    (storage/pg-store! {:entity :invitation
                        :uuid (java.util.UUID/randomUUID)
                        :status "active"
                        :invited-by-id invited-by-id
                        :objective-id objective-id
                        :reason "some reason"
                        :writer-name "writer name"})))

(defn store-a-question []
  (let [{user-id :_id} (store-a-user)
        {objective-id :_id} (store-an-objective)]
    (storage/pg-store! {:entity :question
                        :created-by-id user-id
                        :objective-id objective-id
                        :question "A question"})))
