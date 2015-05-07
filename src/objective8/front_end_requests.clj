(ns objective8.front-end-requests
  (:require [cemerick.friend :as friend]
            [objective8.utils :as utils]
            [objective8.sanitiser :as sanitiser]))

(defn validate [{:keys [request] :as validation-state} field validator]
  (let [{valid :valid reason :reason value :value} (validator request)]
    (cond-> validation-state
      true        (assoc-in [:data field] value)
      (not valid) (assoc-in [:report field] reason)
      (not valid) (assoc :status ::invalid))))

(defn initialise-validation [request]
  {:request request :status ::valid :data {} :report {}})

(defn length-range [value min max]
  (and (>= (count value) min)
       (<= (count value) max)))

(defn objective-title-validator [request]
  (let [title (get-in request [:params :title])]
    (cond-> {:valid true :reason [] :value title}
      (not (length-range title 3 120)) (assoc :valid false :reason :length))))

(defn objective-description-validator [request]
  (let [description (get-in request [:params :description])]
    (cond-> {:valid true :reason [] :value description}
      (not (length-range description 0 5000)) (assoc :valid false :reason :length))))

(defn request->objective-data [{:keys [params] :as request} user-id current-time]
  (let [iso-time (utils/date-time->date-time-plus-30-days current-time)]
    (-> (initialise-validation request)
        (validate :title objective-title-validator)
        (validate :description objective-description-validator)
        (assoc-in [:data :end-date] iso-time)
        (assoc-in [:data :created-by-id] user-id)
        (dissoc :request))))
