(ns objective8.front-end-helpers
  (:require [cemerick.friend :as friend]
            [objective8.utils :as utils]
            [objective8.sanitiser :as sanitiser]))

(defn request->question
  "Returns a map of a question if all parts are in the request. Otherwise returns nil"
  [{:keys [params route-params] :as request} user-id]
  (when-let [objective-id (some-> (:id route-params) 
                                  Integer/parseInt)]
    (some-> params
            (utils/select-all-or-nothing [:question])
            (assoc :created-by-id user-id :objective-id objective-id))))

(defn request->comment-data
  "Returns a map of a comment if all the parts are in the request params."
  [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:comment-on-uri :comment])
          (assoc :created-by-id user-id)))

(defn request->objective
  "Returns a map of an objective if all the parts are in the
  request params. Otherwise returns nil"
  [{:keys [params] :as request} user-id]
    (let [iso-time (utils/date-time->date-time-plus-30-days (utils/current-time))]
      (some-> params
              (utils/select-all-or-nothing [:title :goal-1 :goal-2 :goal-3 :description])
              (assoc :end-date iso-time :created-by-id user-id))))

(defn request->answer-info
  "Returns a map of an answer if all the parts are in the request. Otherwise returns nil"
  [{:keys [params route-params] :as request} user-id]
  (when-let [id-map (some-> route-params
                            (utils/select-all-or-nothing [:id :q-id])
                            (update-in [:id] #(Integer/parseInt %))
                            (update-in [:q-id] #(Integer/parseInt %))
                            (utils/ressoc :id :objective-id)
                            (utils/ressoc :q-id :question-id))] 
    (some-> params
            (utils/select-all-or-nothing [:answer])
            (assoc :created-by-id user-id)
            (merge id-map))))

(defn request->invitation-info
  "Returns a map with the invitation details if all the parts are in the request. Otherwise return nil"
  [{:keys [params route-params] :as request} user-id]
  (when-let [objective-id (some-> (:id route-params)
                                  Integer/parseInt)]
    (some-> params
            (utils/select-all-or-nothing [:writer-name :writer-email :reason])
            (assoc :objective-id objective-id :invited-by-id user-id))))

(defn request->profile-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:name :biog])
          (assoc :user-uri (str "/users/" user-id))))

(defn request->up-vote-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:vote-on-uri])
          (assoc :created-by-id user-id :vote-type "up")))

(defn request->down-vote-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:vote-on-uri])
          (assoc :created-by-id user-id :vote-type "down"))) 

(defn request->star-info [{:keys [params] :as request} user-id]
  (when-let [objective-uri (:objective-uri params)]
    {:objective-uri objective-uri :created-by-id user-id}))

(defn request->draft-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:id :google-doc-html-content])
          (assoc :submitter-id user-id)
          (utils/ressoc :id :objective-id)
          (update-in [:objective-id] #(Integer/parseInt %))
          (utils/ressoc :google-doc-html-content :content)
          (update-in [:content] #(utils/html->hiccup (sanitiser/sanitise-html %)))))

(defn request->mark-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:question-uri])
          (assoc :created-by-uri (str "/users/" user-id))))
