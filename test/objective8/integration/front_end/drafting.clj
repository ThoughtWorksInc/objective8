(ns objective8.integration.front-end.drafting
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [endophile.core :as ec]
            [endophile.hiccup :as eh]
            [hiccup.core :as hc]
            [oauth.client :as oauth]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as ih]
            [objective8.front-end.api.http :as http-api]
            [objective8.config :as config]))

(def TWITTER_ID "twitter-ID")
(def USER_ID 1)
(def OBJECTIVE_ID 2)
(def OBJECTIVE_ID_AS_STRING "2")
(def WRONG_OBJECTIVE_ID (+ OBJECTIVE_ID 100))
(def DRAFT_ID 3)
(def DRAFT_ID_AS_STRING "3")
(def DRAFT_URI (str "/objectives/" OBJECTIVE_ID "/drafts/" DRAFT_ID))
(def SECTION_LABEL "abcdef12")
(def SECTION_LABEL_2 "12abcdef")

(def SOME_MARKDOWN  "A heading\n===\nSome content")
(def SOME_DIFFERENT_MARKDOWN  "Heading\n===\nSome different content\nSome more content")
(def SOME_HICCUP (eh/to-hiccup (ec/mp SOME_MARKDOWN)))
(def SOME_DIFFERENT_HICCUP (eh/to-hiccup (ec/mp SOME_DIFFERENT_MARKDOWN)))
(def SOME_HTML (hc/html SOME_HICCUP))

(def SOME_HICCUP_WITH_LABELS [[:p nil] 
                              [:p {:data-section-label SECTION_LABEL} "first paragraph"] 
                              [:p {:data-section-label SECTION_LABEL_2} "second paragraph"]])

(def add-draft-url (utils/path-for :fe/add-draft-get :id OBJECTIVE_ID))
(def latest-draft-url (utils/path-for :fe/draft :id OBJECTIVE_ID :d-id "latest"))
(def draft-list-url (utils/path-for :fe/draft-list :id OBJECTIVE_ID))
(def draft-diff-url (utils/path-for :fe/draft-diff :id OBJECTIVE_ID :d-id DRAFT_ID))

(def user-session (ih/front-end-context))

(def an-objective {:_id OBJECTIVE_ID
                   :title "my objective title"
                   :description "my objective description"
                   :username "Barry"
                   :uri (str "/objectives/" OBJECTIVE_ID)})

(facts "about writing drafts"
       (against-background
         (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
         (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                         :result {:_id USER_ID
                                                                  :username "username"}}
         (http-api/get-user anything) => {:result {:writer-records [{:objective-id OBJECTIVE_ID}]}})

       (binding [config/enable-csrf false]
         (fact "writer for objective can view add-draft page"
               (-> user-session
                   ih/sign-in-as-existing-user 
                   (p/request add-draft-url)
                   (get-in [:response :status])) => 200
               (provided
                 (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                           :result {:_id 6273 :entity "objective"}}))

         (fact "user who is not a writer for an objective can not view add-draft page"
               (-> user-session
                   ih/sign-in-as-existing-user 
                   (p/request add-draft-url)
                   (get-in [:response :status])) => 403
               (provided
                 (http-api/get-user anything) => {:result {:writer-records [{:objective-id WRONG_OBJECTIVE_ID}]}})) 

         (fact "writer can preview a draft"
               (let [{response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request add-draft-url
                                                         :request-method :post
                                                         :params {:action "preview"
                                                                  :content SOME_MARKDOWN}))]
                 (:status response) => 200
                 (:body response) => (contains SOME_HTML)
                 (:body response) => (contains SOME_MARKDOWN))) 

         (fact "writer can submit a draft"
               (against-background
                 (http-api/post-draft {:objective-id OBJECTIVE_ID
                                       :submitter-id USER_ID
                                       :content SOME_HICCUP}) => {:status ::http-api/success
                                                                  :result {:_id DRAFT_ID}})
               (let [{response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/add-draft-post :id OBJECTIVE_ID)
                                                         :request-method :post
                                                         :params {:action "submit"
                                                                  :content SOME_MARKDOWN}))]
                 (:headers response) => (ih/location-contains (str "/objectives/" OBJECTIVE_ID "/drafts/" DRAFT_ID))
                 (:status response) => 302))))

(facts "about viewing drafts"
       (fact "anyone can view a particular draft"
             (against-background
               (http-api/get-objective OBJECTIVE_ID_AS_STRING) => {:status ::http-api/success
                                                                   :result {:_id OBJECTIVE_ID}}
               (http-api/retrieve-writers OBJECTIVE_ID_AS_STRING) => {:status ::http-api/success}
               (http-api/get-draft OBJECTIVE_ID_AS_STRING DRAFT_ID_AS_STRING) 
               => {:status ::http-api/success
                   :result {:_id DRAFT_ID
                            :_created_at "2015-03-24T17:06:37.714Z"
                            :uri DRAFT_URI
                            :content SOME_HICCUP
                            :objective-id OBJECTIVE_ID
                            :submitter-id USER_ID
                            :username "username"
                            :next-draft-id 4
                            :previous-draft-id 2
                            :meta {:comments-count 0}}}
               (http-api/get-comments anything anything) => {:status ::http-api/success :result {:comments []}})

             (let [{response :response} (p/request user-session (utils/path-for :fe/draft :id OBJECTIVE_ID 
                                                                                :d-id DRAFT_ID))]
               (:status response) => 200
               (:body response) => (contains SOME_HTML)
               (:body response) => (contains (utils/local-path-for :fe/draft :id OBJECTIVE_ID :d-id 2))
               (:body response) => (contains (utils/local-path-for :fe/draft :id OBJECTIVE_ID :d-id 4))))

       (fact "anyone can view latest draft"
             (against-background
               (http-api/get-objective OBJECTIVE_ID_AS_STRING) => {:status ::http-api/success
                                                                   :result {:_id OBJECTIVE_ID}}
               (http-api/retrieve-writers OBJECTIVE_ID_AS_STRING) => {:status ::http-api/success}
               (http-api/get-draft OBJECTIVE_ID_AS_STRING "latest")
               => {:status ::http-api/success
                   :result {:_id DRAFT_ID
                            :_created_at "2015-03-24T17:06:37.714Z"
                            :uri DRAFT_URI
                            :content SOME_HICCUP
                            :objective-id OBJECTIVE_ID
                            :submitter-id USER_ID
                            :meta {:comments-count 0}}} 
               (http-api/get-comments anything anything) => {:status ::http-api/success :result {:comments []}}) 
             (let [{response :response} (p/request user-session latest-draft-url)]
               (:status response) => 200
               (:body response) => (contains SOME_HTML)))

       (fact "anyone can view list of drafts"
             (against-background
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result {:_id OBJECTIVE_ID}}
               (http-api/retrieve-writers OBJECTIVE_ID) => {:status ::http-api/success}
               (http-api/get-all-drafts OBJECTIVE_ID) => {:status ::http-api/success
                                                          :result [{:_id DRAFT_ID
                                                                    :content SOME_HICCUP
                                                                    :objective-id OBJECTIVE_ID
                                                                    :submitter-id USER_ID
                                                                    :_created_at "2015-02-12T16:46:18.838Z"
                                                                    :username "UserName"}]})

             (let [{response :response} (p/request user-session draft-list-url)]
               (:status response) => 200
               (:body response) => (contains "12-02-2015 16:46")
               (:body response) => (contains "UserName")))

       (fact "anyone can view difference between drafts"
             (against-background
               (http-api/get-objective OBJECTIVE_ID_AS_STRING) => {:status ::http-api/success
                                                                   :result {:_id OBJECTIVE_ID}}
               (http-api/get-draft OBJECTIVE_ID_AS_STRING DRAFT_ID_AS_STRING) 
               => {:status ::http-api/success
                   :result {:_id DRAFT_ID
                            :_created_at "2015-03-24T17:06:37.714Z"
                            :content SOME_DIFFERENT_HICCUP
                            :objective-id OBJECTIVE_ID
                            :submitter-id USER_ID
                            :username "username"
                            :next-draft-id 4
                            :previous-draft-id 2}}

               (http-api/get-draft OBJECTIVE_ID_AS_STRING (dec DRAFT_ID)) 
               => {:status ::http-api/success 
                   :result {:_id 2
                            :_created_at "2015-02-24T17:06:37.714Z"
                            :content SOME_HICCUP
                            :objective-id OBJECTIVE_ID
                            :submitter-id USER_ID
                            :username "username"
                            :next-draft-id DRAFT_ID
                            :previous-draft-id 1}})

             (let [{response :response} (p/request user-session draft-diff-url)]
               (:status response) => 200
               (:body response) => (contains "<span>eading</span>")))

       (fact "viewing diff page for the first draft returns 404 page"
             (against-background
               (http-api/get-objective OBJECTIVE_ID_AS_STRING) => {:status ::http-api/success
                                                                   :result {:_id OBJECTIVE_ID}}
               (http-api/get-draft OBJECTIVE_ID_AS_STRING DRAFT_ID_AS_STRING) 
               => {:status ::http-api/success
                   :result {:_id DRAFT_ID
                            :_created_at "2015-03-24T17:06:37.714Z"
                            :content SOME_HICCUP
                            :objective-id OBJECTIVE_ID
                            :submitter-id USER_ID
                            :username "username"
                            :next-draft-id 4
                            :previous-draft-id nil}})

             (let [{response :response} (p/request user-session draft-diff-url)]
               (:status response) => 404))) 

(facts "about rendering draft page"
       (fact "adds section links before elements with section labels"
             (against-background
               (http-api/get-objective OBJECTIVE_ID_AS_STRING) => {:status ::http-api/success
                                                                   :result an-objective} 
               (http-api/get-draft OBJECTIVE_ID_AS_STRING
                                   DRAFT_ID_AS_STRING) => {:status ::http-api/success
                                                           :result {:_id DRAFT_ID
                                                                    :content SOME_HICCUP_WITH_LABELS
                                                                    :objective-id OBJECTIVE_ID
                                                                    :submitter-id USER_ID
                                                                    :_created_at "2015-02-12T16:46:18.838Z"
                                                                    :uri :draft-uri 
                                                                    :username "UserName"
                                                                    :meta {:comments-count 0}}}
               (http-api/get-comments :draft-uri anything) => {:status ::http-api/success 
                                                               :result {:comments []}} 
               (http-api/retrieve-writers OBJECTIVE_ID_AS_STRING) => {:status ::http-api/success 
                                                                      :result []}) 

             (let [user-session (ih/front-end-context)
                   {status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/draft :id OBJECTIVE_ID :d-id DRAFT_ID))
                                                   :response)]
               (count (re-seq #"\"draft-add-inline-comment" body)) => 2
               body => (contains (str "/objectives/" OBJECTIVE_ID "/drafts/" DRAFT_ID "/sections/" SECTION_LABEL))
               body => (contains (str "/objectives/" OBJECTIVE_ID "/drafts/" DRAFT_ID "/sections/" SECTION_LABEL_2)))))

(facts "about rendering import-draft page"
       (against-background
         (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
         (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                         :result {:_id USER_ID
                                                                  :username "username"}}
         (http-api/get-user anything) => {:result {:writer-records [{:objective-id OBJECTIVE_ID}]}} 
         (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                   :result an-objective})
       (let [{status :status body :body} (-> user-session
                                             (ih/sign-in-as-existing-user)
                                             (p/request (utils/path-for :fe/import-draft-get
                                                                        :id OBJECTIVE_ID))
                                             :response)]

         (fact "the cancel link is set correctly" 
               body => (contains (str "href=\"/objectives/" OBJECTIVE_ID "/drafts\"")))
         (fact "the form action is set correctly"
               body => (contains (str "action=\"/objectives/" OBJECTIVE_ID "/import-draft\"")))))

(facts "about rendering draft section page"
       (fact "links back to the draft"
             (against-background
               (http-api/get-draft-section anything) => {:status ::http-api/success
                                                         :result {:section '([:p {:data-section-label SECTION_LABEL} "barry"]) 
                                                                  :uri (str "/objectives/" OBJECTIVE_ID
                                                                            "/drafts/" DRAFT_ID
                                                                            "/sections/" SECTION_LABEL)}})
             (let [user-session (ih/front-end-context)
                   {status :status body :body} (-> user-session
                                                   (p/request (utils/path-for :fe/draft-section :id OBJECTIVE_ID :d-id DRAFT_ID :section-label SECTION_LABEL))
                                                   :response)]
               body => (contains (str "/objectives/" OBJECTIVE_ID "/drafts/" DRAFT_ID "\"")))))
