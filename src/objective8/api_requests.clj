(ns objective8.api-requests)

(defn request->candidate-data [{params :params :as request}]
  params)

(defn request->draft-data [{params :params :as request}]
  (select-keys params [:objective-id :submitter-id :content]))
