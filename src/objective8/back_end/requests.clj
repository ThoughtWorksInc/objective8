(ns objective8.back-end.requests
  (:require [objective8.utils :as utils]))

(defn request->objective-data [{params :params :as request}]
  (select-keys params [:title :description :end-date :created-by-id]))

(defn request->invitation-data [{:keys [params route-params] :as request}]
  (when-let [objective-id (some-> (:id route-params)
                                  Integer/parseInt)]
    (some-> params
            (utils/select-all-or-nothing [:writer-name :writer-email :reason :invited-by-id])
            (assoc :objective-id objective-id))))

(defn request->writer-data [{params :params :as request}]
  params)

(defn request->admin-removal-data [{params :params :as request}]
  (utils/select-all-or-nothing params [:removal-uri :removed-by-uri]))

(defn request->draft-data [{params :params :as request}]
  (select-keys params [:objective-id :submitter-id :content]))

(defn request->up-down-vote-data [{params :params :as request}]
  (some-> (utils/select-all-or-nothing params [:vote-on-uri :created-by-id :vote-type])
          (update-in [:vote-type] keyword)))

(defn request->answer-data [{:keys [route-params params] :as request}]
  {:answer (:answer params)
   :created-by-id (:created-by-id params)
   :objective-id (Integer/parseInt (:id route-params)) 
   :question-id (Integer/parseInt (:q-id route-params))})

(defn request->answers-query [{:keys [params route-params] :as request}]
  (let [question-uri (str "/objectives/" (:id route-params) "/questions/" (:q-id route-params))
        sorted-by (keyword (get params :sorted-by :created-at))
        filter-type (keyword (get params :filter-type :none))]
    (when (and (#{:up-votes :down-votes :created-at} sorted-by)
               (#{:has-writer-note :none} filter-type))
      {:question-uri question-uri
       :sorted-by sorted-by
       :filter-type filter-type})))

(defn request->comment-data [{params :params :as request}]
  (let [reason (:reason params)]
    (cond-> params 
      true (utils/select-all-or-nothing [:comment :created-by-id :comment-on-uri])
      reason (assoc :reason reason))))

(defn get-sorted-by [params]
  (if-let [sorted-by (keyword (:sorted-by params))]
    (if (#{:up-votes :down-votes} sorted-by)
      sorted-by
      :created-at)
    :created-at))

(defn get-filter-type [params]
  (if-let [filter-type (keyword (:filter-type params))]
    (if (#{:has-writer-note} filter-type)
      filter-type
      :none)
    :none))

(defn request->comments-query [{params :params :as request}]
  (some-> (utils/select-all-or-nothing params [:uri])
          (assoc :sorted-by (get-sorted-by params))
          (assoc :filter-type (get-filter-type params))))

(defn request->star-data [{params :params :as request}]
  (utils/select-all-or-nothing params [:objective-uri :created-by-id]))

(defn request->profile-data [{params :params :as request}]
  (utils/select-all-or-nothing params [:name :biog :user-uri]))

(defn request->objectives-query [{:keys [params] :as request}]
  (let [user-id (:user-id params)
        starred (:starred params)
        include-removed? (:include-removed params)] 
    (cond-> {}
      user-id (assoc :signed-in-id (Integer/parseInt user-id))
      starred (assoc :filters {:starred starred})
      include-removed? (assoc :include-removed? include-removed?))))

(defn request->mark-data [{:keys [params] :as request}]
  (utils/select-all-or-nothing params [:question-uri :created-by-uri]))

(defn request->writer-note-data [{:keys [params] :as request}]
  (utils/select-all-or-nothing params [:note :created-by-id :note-on-uri]))
