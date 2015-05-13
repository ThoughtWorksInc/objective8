(ns objective8.front-end-requests
  (:require [cemerick.friend :as friend]
            [clojure.string :as s]
            [objective8.utils :as utils]
            [objective8.sanitiser :as sanitiser]))

(defn validate [{:keys [request] :as validation-state} field validator]
  (when-let [{valid :valid reason :reason value :value} (validator request)]
    (cond-> validation-state
      true        (assoc-in [:data field] value)
      (not valid) (assoc-in [:report field] reason)
      (not valid) (assoc :status ::invalid))))

(defn initialise-request-validation [request]
  {:request request :status ::valid :data {} :report {}})

(defn initialise-field-validation [value]
  {:valid true :reason #{} :value value})

(defn report-error [validator-state reason]
  (-> (assoc validator-state :valid false)
      (update-in [:reason] #(conj % reason))))

(defn longer? [value max]
  (> (count value) max))

(defn shorter? [value min]
  (< (count value) min))

(defn valid-email? [email-address]
  (re-matches #"[^ @]+@[^ @]+$" email-address))

;; Create objective

(defn objective-title-validator [request]
  (let [title (s/trim (get-in request [:params :title]))]
    (cond-> (initialise-field-validation title)
      (shorter? title 3) (report-error :length)
      (longer? title 120) (report-error :length))))

(defn objective-description-validator [request]
  (let [description (s/trim (get-in request [:params :description]))]
    (cond-> (initialise-field-validation description)
      (longer? description 5000) (report-error :length))))

(defn request->objective-data [{:keys [params] :as request} user-id current-time]
  (let [iso-time (utils/date-time->date-time-plus-30-days current-time)]
    (-> (initialise-request-validation request)
        (validate :title objective-title-validator)
        (validate :description objective-description-validator)
        (assoc-in [:data :end-date] iso-time)
        (assoc-in [:data :created-by-id] user-id)
        (dissoc :request))))

;; Create Question

(defn question-validator [request]
  (let [question (s/trim (get-in request [:params :question]))]
    (cond-> (initialise-field-validation question)
      (shorter? question 10) (report-error :length)
      (longer? question 500) (report-error :length))))

(defn request->question-data [{:keys [route-params] :as request} user-id]
  (let [objective-id (some-> route-params :id Integer/parseInt)]
    (-> (initialise-request-validation request)
        (validate :question question-validator)
        (assoc-in [:data :created-by-id] user-id)
        (assoc-in [:data :objective-id] objective-id)
        (dissoc :request))))

;; Create Answer

(defn answer-validator [request]
  (let [answer (s/trim (get-in request [:params :answer]))]
    (cond-> (initialise-field-validation answer)
      (empty? answer)  (report-error :empty)
      (longer? answer 500) (report-error :length))))

(defn request->answer-data [{:keys [params route-params] :as request} user-id]
  (let [objective-id (some-> route-params :id Integer/parseInt)
        question-id (some-> route-params :q-id Integer/parseInt)]
    (-> (initialise-request-validation request)
        (validate :answer answer-validator)
        (assoc-in [:data :created-by-id] user-id)
        (assoc-in [:data :objective-id] objective-id)
        (assoc-in [:data :question-id] question-id)
        (dissoc :request))))

;; Create Comment

(defn comment-validator [request]
  (let [comment (s/trim (get-in request [:params :comment]))]
    (cond-> (initialise-field-validation comment)
      (longer? comment 500) (report-error :length)
      (empty? comment)      (report-error :empty))))

(defn comment-on-uri-validator [request]
  (when-let [comment-on-uri (get-in request [:params :comment-on-uri])]
    (initialise-field-validation comment-on-uri)))

(defn request->comment-data [{:keys [params] :as request} user-id]
  (some-> (initialise-request-validation request)
          (validate :comment comment-validator)
          (validate :comment-on-uri comment-on-uri-validator)
          (assoc-in [:data :created-by-id] user-id)
          (dissoc :request)))

;; Invitations

(defn writer-name-validator [request]
  (let [name (s/trim (get-in request [:params :writer-name]))]
    (cond-> (initialise-field-validation name)
      (empty? name) (report-error :empty)
      (longer? name 50) (report-error :length))))

(defn reason-validator [request]
  (let [reason (s/trim (get-in request [:params :reason]))]
    (cond-> (initialise-field-validation reason)
      (empty? reason) (report-error :empty)
      (longer? reason 5000) (report-error :length))))

(defn writer-email-validator [request]
  (let [writer-email (s/trim (get-in request [:params :writer-email]))]
    (cond-> (initialise-field-validation writer-email)
      (empty? writer-email) (report-error :empty)
      (and (not (empty? writer-email))
           (not (valid-email? writer-email))) (report-error :invalid))))

(defn request->invitation-data [{:keys [route-params] :as request} user-id]
  (when-let [objective-id (some-> route-params :id Integer/parseInt)]
    (some-> (initialise-request-validation request)
            (validate :writer-name writer-name-validator)
            (validate :reason reason-validator)
            (validate :writer-email writer-email-validator)
            (assoc-in [:data :objective-id] objective-id)
            (assoc-in [:data :invited-by-id] user-id)
            (dissoc :request))))

;; Writer notes

(defn note-validator [request]
  (let [note (s/trim (get-in request [:params :note]))]
    (cond-> (initialise-field-validation note)
      (empty? note) (report-error :empty))))

(defn note-on-uri-validator [request]
  (when-let [note-on-uri (get-in request [:params :note-on-uri])]
    (initialise-field-validation note-on-uri)))

(defn request->writer-note-data [{:keys [params] :as request} user-id]
  (some-> (initialise-request-validation request)
          (validate :note note-validator)
          (validate :note-on-uri note-on-uri-validator)
          (assoc-in [:data :created-by-id] user-id)
          (dissoc :request)))

;; Votes

(defn request->up-vote-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:vote-on-uri])
          (assoc :created-by-id user-id :vote-type "up")))

(defn request->down-vote-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:vote-on-uri])
          (assoc :created-by-id user-id :vote-type "down"))) 

;; Stars

(defn request->star-info [{:keys [params] :as request} user-id]
  (when-let [objective-uri (:objective-uri params)]
    {:objective-uri objective-uri :created-by-id user-id})) 

;; Marks

(defn request->mark-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:question-uri])
          (assoc :created-by-uri (str "/users/" user-id))))

;; Admin removals

(defn request->admin-removal-confirmation-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:removal-uri])
          (assoc :removed-by-uri (str "/users/" user-id))))

(defn request->admin-removal-info [{:keys [params] :as request}]
  (utils/select-all-or-nothing params [:removal-uri :removal-sample]))

(defn request->removal-data [{:keys [session] :as request}]
  (some-> (:removal-data session)
          (utils/select-all-or-nothing [:removal-uri :removal-sample])))