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
(def SECTION_LABEL "abcdef12")
(def TWITTER_ID "twitter-123456")

(def participant {:_id USER_ID :username "username" :writer-records []})
(def writer-for-objective {:_id USER_ID :username "username" :writer-records [{:objective-id OBJECTIVE_ID}]})

(def objective {:_id OBJECTIVE_ID :status "open"})
(def objective-in-drafting {:_id OBJECTIVE_ID :status "drafting"})
(def question {:_id QUESTION_ID :objective-id OBJECTIVE_ID})
(def draft {:_id DRAFT_ID
            :objective-id OBJECTIVE_ID
            :_created_at "2015-02-12T16:46:18.838Z"})
(def draft-section {:section '()
                    :uri (str "/objective/" OBJECTIVE_ID "/drafts/" DRAFT_ID "/sections/" SECTION_LABEL)})

(def ^:dynamic the-objective objective)
(def ^:dynamic the-user participant)

(background
 ;; Sign-in background
 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
 (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                 :result the-user}
 (http-api/get-user anything) => {:result the-user}

 ;; Test data background
 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                           :result the-objective}
 (http-api/get-objective OBJECTIVE_ID anything) => {:status ::http-api/success
                                                    :result the-objective}
 (http-api/get-question OBJECTIVE_ID QUESTION_ID) => {:status ::http-api/success
                                                      :result question}
 (http-api/retrieve-answers anything) => {:status ::http-api/success :result []}
 (http-api/get-comments anything) => {:status ::http-api/success :result []}
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
          "A valid title"                  (ih/string-of-length 5001)      "clj-description-length-error")))

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
          (ih/string-of-length 501)   "clj-question-length-error")))

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
          (ih/string-of-length 501)        "clj-answer-length-error")))

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
                    :body) => (contains ?expected-error-message))

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
                      :body) => (contains ?expected-error-message))

            (fact "validation errors are reported when posting a comment on a draft section"
                  (-> user-session
                      ih/sign-in-as-existing-user
                      (p/request (utils/path-for :fe/post-comment)
                                 :request-method :post
                                 :params {:comment ?comment
                                          :comment-on-uri "/thing/to/comment/on"
                                          :refer (utils/local-path-for :fe/draft-section
                                                                       :id OBJECTIVE_ID
                                                                       :d-id DRAFT_ID
                                                                       :section-label SECTION_LABEL)})
                      p/follow-redirect
                      :response
                      :body) => (contains ?expected-error-message)))))

 ?comment                        ?expected-error-message
 ""                              "clj-comment-empty-error"
 (ih/string-of-length 501)       "clj-comment-length-error")

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
          "Jenny"                          "a@b.com"       (ih/string-of-length 5001)        "clj-writer-reason-length-error")))
