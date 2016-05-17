(ns objective8.integration.rendering.translations
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [endophile.core :as ec]
            [endophile.hiccup :as eh]
            [hiccup.core :as hc]
            [objective8.utils :as utils]
            [objective8.front-end.api.http :as http-api]
            [objective8.integration.integration-helpers :as helpers]))

(def OBJECTIVE_ID 1)
(def OBJECTIVE_ID_AS_STRING "1")
(def QUESTION_ID 2)
(def QUESTION_URI (str "/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID))
(def USER_ID 3)
(def DRAFT_ID 4)
(def DRAFT_ID_AS_STRING (str DRAFT_ID))
(def UUID "random-uuid")
(def INVITATION_ID 3)
(def OBJECTIVE_TITLE "some title")
(def ACTIVE_INVITATION {:_id INVITATION_ID
                        :invited-by-id USER_ID
                        :objective-id OBJECTIVE_ID
                        :uuid UUID
                        :status "active"})
(def INVITATION_URL (utils/path-for :fe/writer-invitation :uuid UUID))
(def SOME_MARKDOWN  "A heading\n===\nSome content")
(def SECTION_LABEL "abcdef12")
(def SECTION_LABEL_2 "12abcdef")
(def SOME_HICCUP (eh/to-hiccup (ec/mp SOME_MARKDOWN)))
(def SOME_HICCUP_WITH_LABELS [[:p nil] 
                              [:p {:data-section-label SECTION_LABEL} "first paragraph"] 
                              [:p {:data-section-label SECTION_LABEL_2} "second paragraph"]])
(def SOME_HTML (hc/html SOME_HICCUP))

(def user-session (helpers/front-end-context))

(facts "about rendering index page"
       (fact "there are no untranslated strings"
             (let [{status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/index))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering learn-more page"
       (fact "there are no untranslated strings"
             (let [{status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/learn-more))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering project-status page"
       (fact "there are no untranslated strings"
             (let [{status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/project-status))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(def an-objective {:_id OBJECTIVE_ID
                   :title "my objective title"
                   :description "my objective description"
                   :username "Barry"
                   :uri (str "/objectives/" OBJECTIVE_ID)
                   :meta {:comments-count 100}})

(def promoted-objective (assoc an-objective :promoted true))

(facts "about rendering objective-list page"
       (fact "there are no untranslated strings"
             (against-background
               (http-api/get-objectives) => {:status ::http-api/success
                                             :result [an-objective an-objective promoted-objective]})
             (let [{status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/objective-list))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering create-objective page"
       (fact "there are no untranslated strings"
             (against-background
               (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
               (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                          :result {:_id USER_ID
                                                                                   :username "username"}})
             (let [{status :status body :body} (-> user-session
                                                   (helpers/sign-in-as-existing-user)
                                                   (p/request (utils/path-for :fe/create-objective-form))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering objective page"
       (fact "there are no untranslated strings"
             (against-background 
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result an-objective}
               (http-api/get-comments anything anything)=> {:status ::http-api/success :result []}
               (http-api/retrieve-writers OBJECTIVE_ID) => {:status ::http-api/success :result []}
               (http-api/retrieve-questions OBJECTIVE_ID) => {:status ::http-api/success :result []}) 
             (let [{status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/objective :id OBJECTIVE_ID))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering comments page"
       (fact "there are no untranslated strings for viewing all comments on an objective"
             (against-background 
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result an-objective}
               (http-api/get-comments (:uri an-objective) anything) => {:status ::http-api/success 
                                                                        :result []})
             (let [{status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/get-comments-for-objective 
                                                                              :id OBJECTIVE_ID))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings))

       (fact "there are no untranslated strings for viewing all comments on a draft"
             (against-background
               (http-api/get-objective OBJECTIVE_ID_AS_STRING) => {:status ::http-api/success
                                                                   :result {:_id OBJECTIVE_ID}}
               (http-api/get-draft OBJECTIVE_ID_AS_STRING DRAFT_ID_AS_STRING)
               => {:status ::http-api/success
                   :result {:_id DRAFT_ID
                            :_created_at "2015-02-12T16:46:18.838Z"
                            :meta {:comments-count 5}}}
               (http-api/get-comments anything anything) => {:status ::http-api/success
                                                             :result []})
             (let [{status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/get-comments-for-draft
                                                                              :id OBJECTIVE_ID
                                                                              :d-id DRAFT_ID))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(def a-question {:question "The meaning of life?"
                 :created-by-id USER_ID
                 :uri QUESTION_URI
                 :objective-id OBJECTIVE_ID
                 :_id QUESTION_ID
                 :meta {:answers-count 0}})

(facts "about rendering question page"
       (fact "there are no untranslated strings"
             (against-background 
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result an-objective}
               (http-api/get-question OBJECTIVE_ID QUESTION_ID) => {:status ::http-api/success 
                                                                    :result a-question}
               (http-api/retrieve-answers QUESTION_URI anything) => {:status ::http-api/success
                                                                     :result []})
             (let [{status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/question 
                                                                              :id OBJECTIVE_ID
                                                                              :q-id QUESTION_ID))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering add-question page"
       (fact "there are no untranslated strings"
             (against-background
               (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
               (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                          :result {:_id USER_ID
                                                                                   :username "username"}}
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result an-objective})
             (let [{status :status body :body} (-> user-session
                                                   (helpers/sign-in-as-existing-user)
                                                   (p/request (utils/path-for :fe/add-a-question :id OBJECTIVE_ID))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering invite-writer page"
       (fact "there are no untranslated strings"
             (against-background
               (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
               (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                          :result {:_id USER_ID
                                                                                   :username "username"}}
               (http-api/get-user anything) => {:result {:writer-records [{:objective-id OBJECTIVE_ID}]}}
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result an-objective})
             (let [{status :status body :body} (-> user-session
                                                   (helpers/sign-in-as-existing-user)
                                                   (p/request (utils/path-for :fe/invite-writer :id OBJECTIVE_ID))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering draft-list page"
       (fact "there are no untranslated strings"
             (against-background
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result an-objective} 
               (http-api/get-all-drafts OBJECTIVE_ID) => {:status ::http-api/success
                                                          :result [{:_id DRAFT_ID
                                                                    :content SOME_HICCUP
                                                                    :objective-id OBJECTIVE_ID
                                                                    :submitter-id USER_ID
                                                                    :_created_at "2015-02-12T16:46:18.838Z"
                                                                    :username "UserName"}]} 
               (http-api/retrieve-writers OBJECTIVE_ID) => {:status ::http-api/success 
                                                            :result []}) 
             (let [user-session (helpers/front-end-context)
                   {status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/draft-list :id OBJECTIVE_ID))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering draft page"
       (fact "there are no untranslated strings"
             (against-background
               (http-api/get-objective OBJECTIVE_ID_AS_STRING) => {:status ::http-api/success
                                                                   :result an-objective} 
               (http-api/get-draft OBJECTIVE_ID_AS_STRING DRAFT_ID_AS_STRING) => {:status ::http-api/success
                                                                                  :result {:_id DRAFT_ID
                                                                                           :content SOME_HICCUP_WITH_LABELS
                                                                                           :objective-id OBJECTIVE_ID
                                                                                           :submitter-id USER_ID
                                                                                           :_created_at "2015-02-12T16:46:18.838Z"
                                                                                           :uri :draft-uri 
                                                                                           :username "UserName"
                                                                                           :meta {:comments-count 0}}}
               (http-api/get-comments :draft-uri anything) => {:status ::http-api/success 
                                                               :result []} 
               (http-api/retrieve-writers OBJECTIVE_ID_AS_STRING) => {:status ::http-api/success 
                                                                      :result []}
               (http-api/get-draft-sections :draft-uri) => {:status ::http-api/success
                                                            :result []}) 

             (let [user-session (helpers/front-end-context)
                   {status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/draft :id OBJECTIVE_ID :d-id DRAFT_ID))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings))) 

(facts "about rendering draft diff page"
       (fact "there are no untranslated strings"
             (against-background
               (http-api/get-objective OBJECTIVE_ID_AS_STRING) => {:status ::http-api/success
                                                                   :result an-objective} 
               (http-api/get-draft OBJECTIVE_ID_AS_STRING DRAFT_ID_AS_STRING) 
               => {:status ::http-api/success
                   :result {:_id DRAFT_ID
                            :previous-draft-id (dec DRAFT_ID) 
                            :content SOME_HICCUP
                            :objective-id OBJECTIVE_ID
                            :submitter-id USER_ID
                            :_created_at "2015-02-12T16:46:18.838Z"
                            :uri :draft-uri 
                            :username "UserName"}}
               (http-api/get-draft OBJECTIVE_ID_AS_STRING (dec DRAFT_ID)) 
               => {:status ::http-api/success
                   :result {:_id DRAFT_ID
                            :content SOME_HICCUP
                            :objective-id OBJECTIVE_ID
                            :submitter-id USER_ID
                            :_created_at "2015-02-12T16:46:18.838Z"
                            :uri :draft-uri 
                            :username "UserName"}})
             (let [user-session (helpers/front-end-context)
                   {status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/draft-diff :id OBJECTIVE_ID :d-id DRAFT_ID))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering add-draft page"
       (fact "there are no untranslated strings"
             (against-background
               (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
               (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                          :result {:_id USER_ID
                                                                                   :username "username"}}
               (http-api/get-user anything) => {:result {:writer-records [{:objective-id OBJECTIVE_ID}]}} 
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result an-objective})
             (let [user-session (helpers/front-end-context)
                   {status :status body :body} (-> user-session
                                                   (helpers/sign-in-as-existing-user)
                                                   (p/request (utils/path-for :fe/add-draft-get
                                                                              :id OBJECTIVE_ID))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering import-draft page"
       (fact "there are no untranslated strings" 
             (against-background
               (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
               (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                          :result {:_id USER_ID
                                                                                   :username "username"}}
               (http-api/get-user anything) => {:result {:writer-records [{:objective-id OBJECTIVE_ID}]}} 
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result an-objective})
             (let [{status :status body :body} (-> user-session
                                                   (helpers/sign-in-as-existing-user)
                                                   (p/request (utils/path-for :fe/import-draft-get
                                                                              :id OBJECTIVE_ID))
                                                   :response)]
               status => 200 
               body) => helpers/no-untranslated-strings)) 

(facts "about rendering draft section page"
       (fact "there are no untranslated strings"
             (against-background
               (http-api/get-draft-section anything) => {:status ::http-api/success
                                                         :result {:section '([:p {:data-section-label SECTION_LABEL} "barry"]) 
                                                                  :uri (str "/objectives/" OBJECTIVE_ID
                                                                            "/drafts/" DRAFT_ID
                                                                            "/sections/" SECTION_LABEL)}})
             (let [user-session (helpers/front-end-context)
                   {status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/draft-section :id OBJECTIVE_ID :d-id DRAFT_ID :section-label SECTION_LABEL))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering create-profile page"
       (fact "there are no untranslated strings"
             (against-background
               (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
               (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                          :result {:_id USER_ID
                                                                                   :username "username"}}
               (http-api/retrieve-invitation-by-uuid UUID) => {:status ::http-api/success
                                                               :result ACTIVE_INVITATION}
               (http-api/get-objective OBJECTIVE_ID anything) => {:status ::http-api/success
                                                                  :result {:title OBJECTIVE_TITLE}})
             (let [{status :status body :body} (-> user-session
                                                   helpers/sign-in-as-existing-user
                                                   (p/request INVITATION_URL)
                                                   (p/request (utils/path-for :fe/create-profile-get))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering edit-profile page"
       (fact "there are no untranslated strings"
             (against-background
               (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
               (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                          :result {:_id USER_ID
                                                                                   :username "username"}}
               (http-api/get-user anything) => {:status ::http-api/success
                                                :result {:writer-records [{:objective-id OBJECTIVE_ID}]
                                                         :username "username"
                                                         :profile {:name "Barry"
                                                                   :biog "I'm Barry..."}}})
             (let [{status :status body :body} (-> user-session 
                                                   helpers/sign-in-as-existing-user
                                                   (p/request (utils/path-for :fe/edit-profile-get))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering the writer-dashboard pages"
       (against-background
         (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
         (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                    :result {:_id USER_ID
                                                                             :username "username"}}
         (http-api/get-user anything) => {:status ::http-api/success
                                          :result {:username "username"
                                                   :writer-records [{:objective-id OBJECTIVE_ID}]}})

       (facts "about the questions dashboard"
              (fact "there are no untranslated strings"
                    (against-background
                      (http-api/get-objective anything) => {:status ::http-api/success
                                                            :result (assoc an-objective :meta {:stars 1})}
                      (http-api/retrieve-questions anything anything) => {:status ::http-api/success
                                                                          :result []}
                      (http-api/retrieve-answers anything anything) => {:status ::http-api/success
                                                                        :result []})
                    (let [{status :status body :body} (-> user-session
                                                          helpers/sign-in-as-existing-user
                                                          (p/request (utils/path-for :fe/dashboard-questions :id OBJECTIVE_ID))
                                                          :response)]
                      status => 200
                      body => helpers/no-untranslated-strings)))

       (facts "about the comments dashboard"
              (fact "there are no untranslated strings"
                    (against-background
                      (http-api/get-objective anything) => {:status ::http-api/success
                                                            :result (assoc an-objective :meta {:stars 1})}
                      (http-api/get-all-drafts anything) => {:status ::http-api/success
                                                             :result []}
                      (http-api/get-comments anything anything) => {:status ::http-api/success
                                                                    :result []})
                    (let [{status :status body :body} (-> user-session
                                                          helpers/sign-in-as-existing-user
                                                          (p/request (utils/path-for :fe/dashboard-comments :id OBJECTIVE_ID))
                                                          :response)]
                      status => 200
                      body => helpers/no-untranslated-strings))))

(def CREATED_AT "2015-04-20T10:31:17.343Z")

(facts "about rendering profile page"
       (fact "there are no untranslated strings"
             (against-background
               (http-api/find-user-by-username "username") => {:status ::http-api/success
                                                               :result {:username "username"
                                                                        :_created_at CREATED_AT
                                                                        :_id USER_ID 
                                                                        :profile {:name "Barry"
                                                                                  :biog "I'm Barry..."}}}

               (http-api/get-objectives-for-writer USER_ID) => {:status ::http-api/success
                                                                :result []})
             (let [{status :status body :body} (-> user-session 
                                                   (p/request (utils/path-for :fe/profile :username "username"))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))


(facts "about rendering sign-in page"
       (fact "there are no untranslated strings"
             (let [{status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/sign-in))
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(def twitter-callback-url (str utils/host-url "/twitter-callback?oauth_verifier=VERIFICATION_TOKEN"))

(facts "about rendering sign-up page"
       (fact "there are no untranslated strings"
             (against-background
               (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
               (http-api/find-user-by-auth-provider-user-id "twitter-TWITTER_ID") => {:status ::http-api/not-found})
             (let [{status :status body :body} (-> user-session
                                                   (p/request twitter-callback-url)
                                                   p/follow-redirect
                                                   :response)]
               status => 200
               body => helpers/no-untranslated-strings)))

(facts "about rendering error-404 page"
       (fact "there are no untranslated strings"
             (let [{status :status body :body} (-> user-session
                                                   (p/request (str utils/host-url "/INVALID_ROUTE"))
                                                   :response)]
               status => 404
               body => helpers/no-untranslated-strings)))
