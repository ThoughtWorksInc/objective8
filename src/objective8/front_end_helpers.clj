(ns objective8.front-end-helpers
  (:require [cemerick.friend :as friend]
            [objective8.utils :as utils]
            [objective8.sanitiser :as sanitiser]))

(defn request->question
  "Returns a map of a question if all parts are in the request. Otherwise returns nil"
  [{{id :id} :route-params :keys [params]} 
   user-id]
  (assoc (select-keys params [:question])
          :created-by-id user-id
          :objective-id (Integer/parseInt id)))

(defn request->comment-data
  "Returns a map of a comment if all the parts are in the request params."
  [{:keys [params]} user-id]
  (some-> params
          (utils/select-all-or-nothing [:comment-on-uri :comment])
          (assoc :created-by-id user-id)))

(defn request->objective
  "Returns a map of an objective if all the parts are in the
  request params. Otherwise returns nil"
  [{:keys [params]} user-id]
    (let [iso-time (utils/date-time->date-time-plus-30-days (utils/current-time))]
      (assoc (select-keys params [:title :goal-1 :goal-2 :goal-3 :description ])
                                  :end-date iso-time
                                  :created-by-id user-id)))

(defn request->answer-info
  "Returns a map of an answer if all the parts are in the request. Otherwise returns nil"
  [{{id :id q-id :q-id} :route-params :keys [params]} 
   user-id]
  (assoc (select-keys params [:answer])
         :question-id (Integer/parseInt q-id) 
         :objective-id (Integer/parseInt id) 
         :created-by-id user-id))

(defn request->invitation-info
  "Returns a map with the invitation details if all the parts are in the request. Otherwise return nil"
  [{{id :id} :route-params :keys [params]} 
   user-id]
  (assoc (select-keys params [:writer-name :writer-email :reason])
         :objective-id (Integer/parseInt id)
         :invited-by-id user-id))

(defn request->up-vote-info [request user-id]
  (some-> (:params request)
          (utils/select-all-or-nothing [:vote-on-uri])
          (assoc :created-by-id user-id :vote-type "up")))

(defn request->down-vote-info [request user-id]
  (some-> (:params request)
          (utils/select-all-or-nothing [:vote-on-uri])
          (assoc :created-by-id user-id :vote-type "down"))) 

(defn request->star-info [{:keys [params] :as request} user-id]
  
  (when-let [objective-uri (:objective-uri params)]
    {:objective-uri objective-uri  :created-by-id user-id})) 

(defn request->draft-info [{:keys [params] :as request} user-id]
  (let [objective-id (:id params)
        unsanitised-draft-content (:google-doc-html-content params)]
    (when (and objective-id unsanitised-draft-content user-id)
      {:objective-id (Integer/parseInt objective-id)
       :submitter-id user-id
       :content (utils/html->hiccup (sanitiser/sanitise-html unsanitised-draft-content))})))
