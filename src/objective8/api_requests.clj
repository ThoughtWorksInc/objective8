(ns objective8.api-requests
  (:require [objective8.utils :as utils]))

(defn request->objective-data [{params :params :as request}]
  (select-keys params [:title :goal-1 :goal-2 :goal-3 :description :end-date :created-by-id]))

(defn request->candidate-data [{params :params :as request}]
  params)

(defn request->draft-data [{params :params :as request}]
  (select-keys params [:objective-id :submitter-id :content]))

(defn request->up-down-vote-data [{params :params :as request}]
  (some-> (utils/select-all-or-nothing params [:vote-on-uri :created-by-id :vote-type])
          (update-in [:vote-type] keyword)))

(defn request->comment-data [{params :params :as request}]
  (utils/select-all-or-nothing params [:comment :created-by-id :comment-on-uri]))
