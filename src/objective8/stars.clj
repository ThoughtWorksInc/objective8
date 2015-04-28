(ns objective8.stars
  (:require [objective8.storage.storage :as storage]
            [objective8.storage.uris :as uris]
            [objective8.utils :as utils]))

(defn store-star! [data]
  (some-> data
          (utils/select-all-or-nothing [:objective-id :created-by-id])
          (assoc :entity :star
                 :active true)
          storage/pg-store!))

(defn toggle-star! [star]
  (storage/pg-toggle-star! star))

(defn get-star [objective-uri created-by-uri]
  (let [objective-id (:_id (uris/uri->query objective-uri))
        created-by-id (:_id (uris/uri->query created-by-uri))]
    (-> (storage/pg-retrieve {:entity :star :objective-id objective-id :created-by-id created-by-id})
        :result
        first)))
