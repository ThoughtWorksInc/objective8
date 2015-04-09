(ns objective8.stars
  (:require [objective8.storage.storage :as storage]
            [objective8.utils :as utils]))

(defn store-star! [data]
  (some-> data
          (utils/select-all-or-nothing [:objective-id :created-by-id])
          (assoc :entity :star
                 :active true)
          storage/pg-store!))

(defn retrieve-star [objective-id created-by-id])

