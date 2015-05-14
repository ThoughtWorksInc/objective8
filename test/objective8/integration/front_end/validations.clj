(ns objective8.integration.front-end.validations
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.http-api :as http-api]
            [objective8.integration.integration-helpers :as ih]
            [objective8.config :as config]
            [objective8.utils :as utils]))

(def USER_ID 1)
(def OBJECTIVE_ID 2)
(def QUESTION_ID 3)
(def DRAFT_ID 4)
(def INVITATION_ID 5)
(def INVITATION_UUID "SOME_UUID")
(def SECTION_LABEL "abcdef12")
(def TWITTER_ID "twitter-123456")


(def participant {:_id USER_ID :username "username" :writer-records []})
(def writer-for-objective {:_id USER_ID :username "username" :writer-records [{:objective-id OBJECTIVE_ID}]})

(def objective {:_id OBJECTIVE_ID :status "open"})
(def objective-in-drafting {:_id OBJECTIVE_ID :status "drafting"})
(def question {:_id QUESTION_ID :objective-id OBJECTIVE_ID :question "Why?"})
(def draft {:_id DRAFT_ID
            :objective-id OBJECTIVE_ID
            :_created_at "2015-02-12T16:46:18.838Z"})
(def draft-section {:section '()
                    :uri (str "/objective/" OBJECTIVE_ID "/drafts/" DRAFT_ID "/sections/" SECTION_LABEL)})
(def the-invitation {:uuid INVITATION_UUID
                     :objective-id OBJECTIVE_ID 
                     :invitation-id INVITATION_ID
                     :status "active"})

(def ^:dynamic the-objective objective)
(def ^:dynamic the-user participant)


(background
 ;; Sign-in background
 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
 (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                 :result the-user}
 (http-api/get-user anything) => {:status ::http-api/success
                                  :result the-user}

 ;; Test data background
 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                           :result the-objective}
 (http-api/get-objective OBJECTIVE_ID anything) => {:status ::http-api/success
                                                    :result the-objective}
 (http-api/get-question OBJECTIVE_ID QUESTION_ID) => {:status ::http-api/success
                                                      :result question}
 (http-api/retrieve-answers anything) => {:status ::http-api/success :result []}
 (http-api/get-comments anything) => {:status ::http-api/success :result []}
 (http-api/retrieve-invitation-by-uuid INVITATION_UUID) => {:status ::http-api/success :result the-invitation}
 (http-api/retrieve-writers OBJECTIVE_ID) => {:status ::http-api/success :result []}
 (http-api/retrieve-questions OBJECTIVE_ID) => {:status ::http-api/success :result []}
 (http-api/get-draft OBJECTIVE_ID DRAFT_ID) => {:status ::http-api/success
                                                :result draft}
 (http-api/get-draft-section anything) => {:status ::http-api/success :result draft-section})


(def user-session (ih/test-context))

(facts "about the create objective form"
       (binding [config/enable-csrf false]
         (tabular
          (fact "validation errors are reported"
                (-> user-session 
                    ih/sign-in-as-existing-user
                    (p/request (utils/path-for :fe/create-objective-form-post)
                               :request-method :post
                               :params {:title ?title
                                        :description ?description})
                    p/follow-redirect
                    :response
                    :body) => (contains ?expected-error-message))
          
          ?title                           ?description                    ?expected-error-message
          "12"                             "A description"                 "clj-title-length-error"
          (ih/string-of-length 121)        "A description"                 "clj-title-length-error"
          "A valid title"                  (ih/string-of-length 5001)      "clj-description-length-error")

         (tabular
          (fact "validation errors are hidden by default"
                (let [objective-form-html (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/create-objective-form))
                                              :response
                                              :body)]
                  objective-form-html =not=> (contains ?error-tag)))
          ?error-tag "clj-title-length-error" "clj-description-length-error")))

(facts "about the add question form"
       (binding [config/enable-csrf false]
         (tabular
          (fact "validation errors are reported"
                (-> user-session
                    ih/sign-in-as-existing-user
                    (p/request (utils/path-for :fe/add-question-form-post :id OBJECTIVE_ID)
                               :request-method :post
                               :params {:question ?question})
                    p/follow-redirect
                    :response
                    :body) => (contains ?expected-error-message))
          
          ?question                        ?expected-error-message
          (ih/string-of-length 9)     "clj-question-length-error"
          (ih/string-of-length 501)   "clj-question-length-error")

         (tabular
          (fact "validation errors are hidden by default"
                (let [question-form-html (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/add-a-question :id OBJECTIVE_ID))
                                              :response
                                              :body)]
                  question-form-html =not=> (contains ?error-tag)))
          ?error-tag "clj-question-length-error")))

(facts "about the add answer form"
       (binding [config/enable-csrf false]
         (tabular
          (fact "validation errors are reported"
                (-> user-session
                    ih/sign-in-as-existing-user
                    (p/request (utils/path-for :fe/add-answer-form-post :id OBJECTIVE_ID :q-id QUESTION_ID)
                               :request-method :post
                               :params {:answer ?answer})
                    p/follow-redirect
                    :response
                    :body) => (contains ?expected-error-message))
          
          ?answer                          ?expected-error-message
          ""                               "clj-answer-empty-error"
          (ih/string-of-length 501)        "clj-answer-length-error")

         (tabular
          (fact "validation errors are hidden by default"
                (let [answer-form-html (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/question :id OBJECTIVE_ID :q-id QUESTION_ID))
                                              :response
                                              :body)]
                  answer-form-html =not=> (contains ?error-tag)))
          ?error-tag "clj-answer-length-error" "clj-answer-empty-error")))

(facts "about the create profile form"
       (binding [config/enable-csrf false
                 the-user           writer-for-objective] 
         (tabular
           (fact "validation errors are reported"
                 (-> user-session
                     ih/sign-in-as-existing-user
                     (p/request (utils/path-for :fe/writer-invitation
                                                :uuid INVITATION_UUID))
                     (p/request (utils/path-for :fe/create-profile-post)
                                :request-method :post
                                :params {:name ?name
                                         :biog ?biog})
                     p/follow-redirect
                     :response
                     :body) => (contains ?expected-error-message))

           ?name                      ?biog                       ?expected-error-message
           ""                         "valid biography"           "clj-name-empty-error"
           (ih/string-of-length 51)   "valid biography"           "clj-name-length-error"
           "Peter Profile"            ""                          "clj-biog-empty-error"
           "Peter Profile"            (ih/string-of-length 5001)  "clj-biog-length-error")
         
         (tabular
          (fact "validation errors are hidden by default"
                (let [create-profile-form-html (-> user-session
                                                 ih/sign-in-as-existing-user
                                                 (p/request (utils/path-for :fe/create-profile-get))
                                                 :response
                                                 :body)]
                  create-profile-form-html =not=> (contains ?error-tag)))
           ?error-tag "clj-name-empty-error" "clj-name-length-error" "clj-biog-empty-error" "clj-biog-length-error")))

(facts "about the edit profile form"
       (binding [config/enable-csrf false
                 the-user           writer-for-objective] 
         (tabular
           (fact "validation errors are reported"
                 (-> user-session
                     ih/sign-in-as-existing-user
                     (p/request (utils/path-for :fe/edit-profile-post)
                                :request-method :post
                                :params {:name ?name
                                         :biog ?biog})
                     p/follow-redirect
                     :response
                     :body) => (contains ?expected-error-message))

           ?name                      ?biog                       ?expected-error-message
           ""                         "valid biography"           "clj-name-empty-error"
           (ih/string-of-length 51)   "valid biography"           "clj-name-length-error"
           "Peter Profile"            ""                          "clj-biog-empty-error"
           "Peter Profile"            (ih/string-of-length 5001)  "clj-biog-length-error")
         
         (tabular
          (fact "validation errors are hidden by default"
                (let [edit-profile-form-html (-> user-session
                                                 ih/sign-in-as-existing-user
                                                 (p/request (utils/path-for :fe/edit-profile-get))
                                                 :response
                                                 :body)]
                  edit-profile-form-html =not=> (contains ?error-tag)))
           ?error-tag "clj-name-empty-error" "clj-name-length-error" "clj-biog-empty-error" "clj-biog-length-error")))

(tabular
 (facts "about creating comments"
        (binding [config/enable-csrf false]
          (fact "validation errors are reported when posting a comment from the objective details page"
                (-> user-session
                    ih/sign-in-as-existing-user
                    (p/request (utils/path-for :fe/post-comment)
                               :request-method :post
                               :params {:comment ?comment
                                        :comment-on-uri "/thing/to/comment/on"
                                        :refer (utils/local-path-for :fe/objective :id OBJECTIVE_ID)})
                    p/follow-redirect
                    :response
                    :body) => (contains ?error-tag))

          (fact "comment validation errors are hidden by default on the objective detail page"
                (let [objective-details-html (-> user-session
                                                 ih/sign-in-as-existing-user
                                                 (p/request (utils/path-for :fe/objective :id OBJECTIVE_ID))
                                                 :response
                                                 :body)]
                  objective-details-html =not=> (contains ?error-tag)))

          (binding [the-objective objective-in-drafting]
            (fact "validation errors are reported when posting a comment on a draft"
                  (-> user-session
                      ih/sign-in-as-existing-user
                      (p/request (utils/path-for :fe/post-comment)
                                 :request-method :post
                                 :params {:comment ?comment
                                          :comment-on-uri "/thing/to/comment/on"
                                          :refer (utils/local-path-for :fe/draft :id OBJECTIVE_ID :d-id DRAFT_ID)})
                      p/follow-redirect
                      :response
                      :body) => (contains ?error-tag))

            (fact "comment validation errors are hidden by default on draft view pages"
                  (let [draft-html (-> user-session
                                       ih/sign-in-as-existing-user
                                       (p/request (utils/path-for :fe/draft :id OBJECTIVE_ID :d-id DRAFT_ID))
                                       :response
                                       :body)]
                    draft-html =not=> (contains ?error-tag)))

            (fact "validation errors are reported when posting a comment on a draft section"
                  (-> user-session
                      ih/sign-in-as-existing-user
                      (p/request (utils/path-for :fe/post-annotation
                                                 :id OBJECTIVE_ID
                                                 :d-id DRAFT_ID
                                                 :section-label SECTION_LABEL)

                                 :request-method :post
                                 :params {:comment ?comment
                                          :reason "general"
                                          :comment-on-uri "/thing/to/comment/on"
                                          :refer (utils/local-path-for :fe/draft-section
                                                                       :id OBJECTIVE_ID
                                                                       :d-id DRAFT_ID
                                                                       :section-label SECTION_LABEL)})
                      p/follow-redirect
                      :response
                      :body) => (contains ?error-tag))

            (fact "comment validation errors are hidden by default on draft section pages"
                  (let [draft-section-html (-> user-session
                                       ih/sign-in-as-existing-user
                                       (p/request (utils/path-for :fe/draft-section
                                                                  :id OBJECTIVE_ID
                                                                  :d-id DRAFT_ID
                                                                  :section-label SECTION_LABEL))
                                       :response
                                       :body)]
                    draft-section-html =not=> (contains ?error-tag))))))

 ?comment                        ?error-tag
 ""                              "clj-comment-empty-error"
 (ih/string-of-length 501)       "clj-comment-length-error")

(facts "about importing drafts"
       (binding [config/enable-csrf false
                 the-user writer-for-objective
                 the-objective objective-in-drafting]
          (fact "validation errors are reported"
                (-> user-session
                    ih/sign-in-as-existing-user
                    (p/request (utils/path-for :fe/import-draft-post :id OBJECTIVE_ID)
                               :request-method :post
                               :params {:google-doc-html-content ""})
                    p/follow-redirect
                    :response
                    :body) => (contains "clj-draft-content-empty-error"))
          
          (fact "validation errors are hidden by default"
                (let [import-draft-html (-> user-session
                                            ih/sign-in-as-existing-user
                                            (p/request (utils/path-for :fe/import-draft-get :id OBJECTIVE_ID))
                                            :response
                                            :body)]
                  import-draft-html =not=> (contains "clj-draft-content-empty-error")))))

(facts "about adding drafts"
       (binding [config/enable-csrf false
                 the-user writer-for-objective
                 the-objective objective-in-drafting]
          (fact "validation errors are reported"
                (-> user-session
                    ih/sign-in-as-existing-user
                    (p/request (utils/path-for :fe/add-draft-post :id OBJECTIVE_ID)
                               :request-method :post
                               :params {:content ""})
                    p/follow-redirect
                    :response
                    :body) => (contains "clj-draft-empty-error"))
          
          (fact "validation errors are hidden by default"
                (let [add-draft-html (-> user-session
                                            ih/sign-in-as-existing-user
                                            (p/request (utils/path-for :fe/add-draft-get :id OBJECTIVE_ID))
                                            :response
                                            :body)]
                  add-draft-html =not=> (contains "clj-draft-empty-error")))))

(facts "about inviting writers"
       (binding [config/enable-csrf false
                 the-user           writer-for-objective]
         (tabular
          (fact "validation errors are reported"
                (-> user-session
                    ih/sign-in-as-existing-user
                    (p/request (utils/path-for :fe/invitation-form-post :id OBJECTIVE_ID)
                               :request-method :post
                               :params {:writer-name ?writer-name
                                        :writer-email ?writer-email
                                        :reason ?reason})
                    p/follow-redirect
                    :response
                    :body) => (contains ?expected-error-message))
          
          ?writer-name                     ?writer-email   ?reason                           ?expected-error-message
          ""                               "a@b.com"       "a reason"                        "clj-writer-name-empty-error"
          (ih/string-of-length 51)         "a@b.com"       "a reason"                        "clj-writer-name-length-error"
          "Jenny"                          ""              "a reason"                        "clj-writer-email-empty-error"
          "Jenny"                          "invalid-email" "a reason"                        "clj-writer-email-invalid-error"
          "Jenny"                          "a@b.com"       ""                                "clj-writer-reason-empty-error"
          "Jenny"                          "a@b.com"       (ih/string-of-length 5001)        "clj-writer-reason-length-error")

         (tabular
          (fact "validation errors are hidden by default"
                (let [invitation-form-html (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/invite-writer :id OBJECTIVE_ID))
                                              :response
                                              :body)]
                  invitation-form-html =not=> (contains ?error-tag)))
          ?error-tag
          "clj-writer-name-empty-error"
          "clj-writer-name-length-error" 
          "clj-writer-email-empty-error"  
          "clj-writer-email-invalid-error"
          "clj-writer-reason-empty-error" 
          "clj-writer-reason-length-error")))

(tabular
 (facts "about writer notes"
        (binding [config/enable-csrf false
                  the-user           writer-for-objective]
          (facts "on the questions dashboard"
                 (against-background
                   (http-api/retrieve-questions OBJECTIVE_ID anything) => {:status ::http-api/success
                                                                           :result [{}]}
                   (http-api/retrieve-answers anything anything) => {:status ::http-api/success
                                                                     :result [{:uri "/answer/uri"}]})
                 (fact "validation errors are reported"
                       (-> user-session
                           ih/sign-in-as-existing-user
                           (p/request (utils/path-for :fe/post-writer-note)
                                      :request-method :post
                                      :params {:note ?note
                                               :note-on-uri "/answer/uri"
                                               :refer (utils/local-path-for :fe/dashboard-questions :id OBJECTIVE_ID)})
                           p/follow-redirect
                           :response
                           :body) => (contains ?error-tag))

                 (fact "validation errors are hidden by default"
                       (let [dashboard-html (-> user-session
                                                ih/sign-in-as-existing-user
                                                (p/request (utils/path-for :fe/dashboard-questions :id OBJECTIVE_ID))
                                                :response
                                                :body)]
                         dashboard-html =not=> (contains ?error-tag))))


          (facts "on the comments dashboard"
                 (against-background
                   (http-api/get-all-drafts anything) => {:status ::http-api/success
                                                          :result [{:_created_at "2015-04-04T12:00:00.000Z"}]}
                   (http-api/get-comments anything anything) => {:status ::http-api/success
                                                                 :result [{:uri "/comment/uri"
                                                                           :_created_at "2015-01-01T01:01:00.000Z"}]})
                 (fact "validation errors are reported"
                       (-> user-session
                           ih/sign-in-as-existing-user
                           (p/request (utils/path-for :fe/post-writer-note)
                                      :request-method :post
                                      :params {:note ?note
                                               :note-on-uri "/comment/uri"
                                               :refer (utils/local-path-for :fe/dashboard-comments :id OBJECTIVE_ID)})
                           p/follow-redirect
                           :response
                           :body) => (contains ?error-tag))

                 (fact "validation errors are hidden by default"
                       (let [dashboard-html (-> user-session
                                                ih/sign-in-as-existing-user
                                                (p/request (utils/path-for :fe/dashboard-comments :id OBJECTIVE_ID))
                                                :response
                                                :body)]
                         dashboard-html =not=> (contains ?error-tag))))))

 ?note     ?error-tag
 ""        "clj-writer-note-empty-error")
