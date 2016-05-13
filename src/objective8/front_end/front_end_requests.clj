(ns objective8.front-end.front-end-requests
  (:require [cemerick.friend :as friend]
            [clojure.string :as s]
            [clojure.tools.logging :as log]
            [objective8.utils :as utils]
            [objective8.front-end.sanitiser :as sanitiser]))

(defn validate [{:keys [request] :as validation-state} field validator]
  (when-let [{valid :valid reason :reason value :value} (validator request)]
    (when-not valid
      (log/info (str "Invalid front-end request. Validator: " (last (s/split (str validator) #"\$")) 
                     ", Field: " field ", Reason: " reason "")))
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

(defn integer-string? [id]
 (re-matches #"-?\d+" id))

(defn valid-email? [email-address]
  (and (re-matches #"[^ @]+@[^ @]+$" email-address) (shorter? email-address 257)))

(defn valid-not-empty-email? [email]
  (and (not (empty? email)) (valid-email? email)))

(defn valid-username? [username]
  (re-matches #"[a-zA-Z0-9]{1,16}" username))

(defn id-validator [request]
  (let [id (get-in request [:route-params :id])]
    (when (integer-string? id)
      (initialise-field-validation id))))

;; User sign-up

(defn username-validator [request]
  (let [username (s/trim (get-in request [:params :username] ""))]
    (cond-> (initialise-field-validation username)
      (not (valid-username? username)) (report-error :invalid))))

(defn email-address-validator [request]
  (let [email-address (s/trim (get-in request [:params :email-address] ""))]
    (cond-> (initialise-field-validation email-address)
      (empty? email-address) (report-error :empty)
      (and (not (empty? email-address))
           (not (valid-email? email-address))) (report-error :invalid))))

(defn include-auth-email [{:keys [params session] :as request}]
  (let [auth-email (:auth-provider-user-email session)
        user-email (:email-address params)]
    (if (and auth-email (nil? user-email))
      (assoc-in request [:params :email-address] auth-email)
      request)))

(defn request->user-sign-up-data [request]
  (let [updated-request (include-auth-email request)]
    (-> (initialise-request-validation updated-request)
        (validate :username username-validator)
        (validate :email-address email-address-validator)
        (dissoc :request))))

;; Create objective

(defn objective-title-validator [request]
  (let [title (s/trim (get-in request [:params :title] ""))]
    (cond-> (initialise-field-validation title)
      (shorter? title 3) (report-error :length)
      (longer? title 120) (report-error :length))))

(defn objective-description-validator [request]
  (let [description (s/trim (get-in request [:params :description] ""))]
    (cond-> (initialise-field-validation description)
      (empty? description) (report-error :empty)
      (longer? description 5000) (report-error :length))))

(defn request->objective-data [request user-id]
  (-> (initialise-request-validation request)
      (validate :title objective-title-validator)
      (validate :description objective-description-validator)
      (assoc-in [:data :created-by-id] user-id)
      (dissoc :request)))

;; Create Question

(defn question-validator [request]
  (let [question (s/trim (get-in request [:params :question] ""))]
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
  (let [answer (s/trim (get-in request [:params :answer] ""))]
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
  (let [comment (s/trim (get-in request [:params :comment] ""))]
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

;; Create Annotation

(def annotation-reasons '("general" "unclear" "expand" "suggestion" "language"))

(defn annotation-reason-validator [request]
  (let [reason (s/trim (get-in request [:params :reason] ""))]
    (cond-> (initialise-field-validation reason)
      (not (some #{reason} annotation-reasons)) (report-error :incorrect-type))))

(defn request->annotation-data [request user-id]
  (some-> (initialise-request-validation request)
          (validate :comment comment-validator)
          (validate :comment-on-uri comment-on-uri-validator)
          (validate :reason annotation-reason-validator)
          (assoc-in [:data :created-by-id] user-id)
          (dissoc :request)))

;; Invitations

(defn writer-name-validator [request]
  (let [name (s/trim (get-in request [:params :writer-name] ""))]
    (cond-> (initialise-field-validation name)
      (empty? name) (report-error :empty)
      (longer? name 50) (report-error :length))))

(defn reason-validator [request]
  (let [reason (s/trim (get-in request [:params :reason] ""))]
    (cond-> (initialise-field-validation reason)
      (empty? reason) (report-error :empty)
      (longer? reason 5000) (report-error :length))))

(defn writer-email-validator [request]
  (let [writer-email (s/trim (get-in request [:params :writer-email] ""))]
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
  (let [note (s/trim (get-in request [:params :note] ""))]
    (cond-> (initialise-field-validation note)
      (empty? note) (report-error :empty)
      (longer? note 500) (report-error :length))))

(defn note-on-uri-validator [request]
  (when-let [note-on-uri (get-in request [:params :note-on-uri])]
    (initialise-field-validation note-on-uri)))

(defn request->writer-note-data [{:keys [params] :as request} user-id]
  (some-> (initialise-request-validation request)
          (validate :note note-validator)
          (validate :note-on-uri note-on-uri-validator)
          (assoc-in [:data :created-by-id] user-id)
          (dissoc :request)))

;; Profiles

(defn name-validator [request]
  (let [name (s/trim (get-in request [:params :name] ""))]
    (cond-> (initialise-field-validation name)
      (empty? name) (report-error :empty)
      (longer? name 50) (report-error :length))))

(defn biog-validator [request]
  (let [biog (s/trim (get-in request [:params :biog] ""))]
    (cond-> (initialise-field-validation biog)
      (empty? biog) (report-error :empty)
      (longer? biog 5000) (report-error :length))))

(defn request->profile-data [{:keys [params] :as request} user-id]
  (some-> (initialise-request-validation request)
          (validate :name name-validator)
          (validate :biog biog-validator)
          (assoc-in [:data :user-uri] (str "/users/" user-id))
          (dissoc :request)))

;; Drafts

(defn valid-draft-id? [draft-id]
  (or (= "latest" draft-id) (integer-string? draft-id)))

(defn draft-id-validator [request]
  (let [draft-id (get-in request [:route-params :d-id])]
    (when (valid-draft-id? draft-id)
      (initialise-field-validation draft-id))))

(defn offset-validator [request]
  (let [offset (get-in request [:params :offset] "0")]
    (cond-> (initialise-field-validation offset)
      (or (not (integer-string? offset))
          (< (Integer/parseInt offset) 0)) (report-error :negative)
      (not (integer-string? offset)) (report-error :non-int))))

;;; Retrieving drafts

(defn request->draft-query [request]
  (some-> (initialise-request-validation request)
          (validate :objective-id id-validator)
          (validate :draft-id draft-id-validator)
          (dissoc :request)))

(defn request->draft-comments-query [request]
  (some-> (initialise-request-validation request)
          (validate :objective-id id-validator)
          (validate :draft-id draft-id-validator)
          (validate :offset offset-validator)
          (dissoc :request)))

;;; Adding drafts
(defn add-draft-markdown-validator [request]
  (let [content (s/trim (get-in request [:params :content] ""))]
    (cond-> (initialise-field-validation content)
      (empty? content) (report-error :empty))))

(defn add-draft-generate-hiccup [validation-state]
  (let [markdown (get-in validation-state [:data :markdown])]
    (assoc-in validation-state [:data :hiccup]
              (utils/markdown->hiccup markdown))))

(defn request->add-draft-data [request user-id]
  (let [objective-id (Integer/parseInt (get-in request [:route-params :id]))]
    (some-> (initialise-request-validation request)
            (validate :markdown add-draft-markdown-validator)
            (assoc-in [:data :objective-id] objective-id)
            (assoc-in [:data :submitter-id] user-id)
            add-draft-generate-hiccup
            (dissoc :request))))

;;; Importing drafts
(defn import-draft-content-validator [request]
  (let [html-content (s/trim (get-in request [:params :google-doc-html-content] ""))]
    (cond-> (initialise-field-validation html-content)
      (empty? html-content) (report-error :empty))))

(defn request->imported-draft-data [request user-id]
  (let [objective-id (Integer/parseInt (get-in request [:route-params :id]))]
    (some-> (initialise-request-validation request)
            (validate :content import-draft-content-validator)
            (assoc-in [:data :objective-id] objective-id)
            (assoc-in [:data :submitter-id] user-id)
            (update-in [:data :content] (comp utils/html->hiccup sanitiser/sanitise-html))
            (dissoc :request))))



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

;; Promoting objectives

(defn request->promoted-data [{:keys [params] :as request} user-id]
  (when-let [objective-uri (:objective-uri params)]
    {:objective-uri objective-uri :promoted-by (str "/users/" user-id)}))
