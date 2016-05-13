(ns objective8.back-end.requests
  (:require [clj-time.format :as time-format]
            [objective8.utils :as utils]))

(defn request->objective-data [{params :params :as request}]
  (select-keys params [:title :description :created-by-id]))

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

(defn request->comment-data [{params :params :as request}]
  (let [reason (:reason params)]
    (cond-> params
            true (utils/select-all-or-nothing [:comment :created-by-id :comment-on-uri])
            reason (assoc :reason reason))))

(defn validate-sorted-by [query sorted-by]
  (if sorted-by
    (when (#{:up-votes :down-votes :created-at} sorted-by)
      (assoc query :sorted-by sorted-by))
    query))

(defn validate-filter-type [query filter-type]
  (if filter-type
    (when (#{:has-writer-note :none} filter-type)
      (assoc query :filter-type filter-type))
    query))

(defn validate-limit [query limit-string]
  (if limit-string
    (try
      (let [limit (Integer/parseInt limit-string)]
        (when (> limit 0)
          (assoc query :limit limit)))
      (catch Exception e
        nil))
    query))

(defn validate-offset [query offset-string]
  (if offset-string
    (try
      (let [offset (Integer/parseInt offset-string)]
        (when (>= offset 0)
          (assoc query :offset offset)))
      (catch Exception e
        nil))
    query))

(defn validate-date [date]
  (if (time-format/parse date)
    date
    :invalid))

(defn request->answer-data [{:keys [route-params params] :as request}]
  {:answer        (:answer params)
   :created-by-id (:created-by-id params)
   :objective-id  (Integer/parseInt (:id route-params))
   :question-id   (Integer/parseInt (:q-id route-params))})

(defn request->answers-query [{:keys [params route-params] :as request}]
  (let [question-uri (str "/objectives/" (:id route-params) "/questions/" (:q-id route-params))
        sorted-by (keyword (get params :sorted-by :created-at))
        filter-type (keyword (get params :filter-type :none))
        offset (:offset params)]
    (some-> {:question-uri question-uri}
            (validate-sorted-by sorted-by)
            (validate-filter-type filter-type)
            (validate-offset offset))))

(defn request->comments-query [{params :params :as request}]
  (let [sorted-by (keyword (:sorted-by params))
        filter-type (keyword (:filter-type params))
        limit (:limit params)
        offset (:offset params)]
    (some-> (utils/select-all-or-nothing params [:uri])
            (validate-sorted-by sorted-by)
            (validate-filter-type filter-type)
            (validate-limit limit)
            (validate-offset offset))))

(defn request->star-data [{params :params :as request}]
  (utils/select-all-or-nothing params [:objective-uri :created-by-id]))

(defn request->promote-objective-data [{params :params :as request}]
  (utils/select-all-or-nothing params [:objective-uri :promoted-by]))

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

(defn request->activities-query [request]
  (try
    {:limit     (when-let [limit (get-in request [:params :limit])] (Integer/parseInt limit))
     :offset    (when-let [offset (get-in request [:params :offset])] (Integer/parseInt offset))
     :wrapped   (= "true" (get-in request [:params :as_ordered_collection]))
     :from-date (when-let [from-date (get-in request [:params :from])] (validate-date from-date))
     :to-date   (when-let [to-date (get-in request [:params :to])] (validate-date to-date))}
    (catch Exception e nil)))


