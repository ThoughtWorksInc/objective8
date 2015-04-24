(ns objective8.api-requests
  (:require [objective8.utils :as utils]))

(defn request->objective-data [{params :params :as request}]
  (select-keys params [:title :goal-1 :goal-2 :goal-3 :description :end-date :created-by-id]))

(defn request->invitation-data [{:keys [params route-params] :as request}]
  (when-let [objective-id (some-> (:id route-params)
                                  Integer/parseInt)]
    (some-> params
            (utils/select-all-or-nothing [:writer-name :writer-email :reason :invited-by-id])
            (assoc :objective-id objective-id))))

(defn request->writer-data [{params :params :as request}]
  params)

(defn request->draft-data [{params :params :as request}]
  (select-keys params [:objective-id :submitter-id :content]))

(defn request->up-down-vote-data [{params :params :as request}]
  (some-> (utils/select-all-or-nothing params [:vote-on-uri :created-by-id :vote-type])
          (update-in [:vote-type] keyword)))

(defn request->comment-data [{params :params :as request}]
  (utils/select-all-or-nothing params [:comment :created-by-id :comment-on-uri]))

(defn request->star-data [{params :params :as request}]
  (utils/select-all-or-nothing params [:objective-uri :created-by-id]))

(defn request->profile-data [{params :params :as request}]
  (utils/select-all-or-nothing params [:name :biog :user-uri]))

(defn request->objectives-query [{:keys [params] :as request}]
  (let [user-id (:user-id params)
        starred (:starred params)] 
    (cond-> {}
      user-id (assoc :signed-in-id (Integer/parseInt user-id))
      starred (assoc :filters {:starred starred}))))

(defn request->mark-data [{:keys [params] :as request}]
  (utils/select-all-or-nothing params [:question-uri :created-by-uri]))

(defn request->writer-note-data [{:keys [params] :as request}]
  (utils/select-all-or-nothing params [:note :created-by-id :note-on-uri]))
