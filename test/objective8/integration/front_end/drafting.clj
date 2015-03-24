(ns objective8.integration.front-end.drafting
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [endophile.core :as ec]
            [endophile.hiccup :as eh]
            [hiccup.core :as hc]
            [oauth.client :as oauth]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as ih]
            [objective8.http-api :as http-api]
            [objective8.config :as config]))

(def TWITTER_ID "twitter-ID")
(def USER_ID 1)
(def OBJECTIVE_ID 2)
(def WRONG_OBJECTIVE_ID (+ OBJECTIVE_ID 100))
(def DRAFT_ID 3)

(def SOME_MARKDOWN  "A heading\n===\nSome content")
(def SOME_HICCUP (eh/to-hiccup (ec/mp SOME_MARKDOWN)))
(def SOME_HTML (hc/html SOME_HICCUP))

(def add-draft-url (utils/path-for :fe/add-draft-get :id OBJECTIVE_ID))
(def latest-draft-url (utils/path-for :fe/draft :id OBJECTIVE_ID :d-id "latest"))
(def draft-list-url (utils/path-for :fe/draft-list :id OBJECTIVE_ID))

(def user-session (ih/test-context))

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
                                                           :result {:_id 6273 :drafting-started true :entity "objective"}})) 

         (fact "add-draft page can not be reached when objective is not in drafting"
               (-> user-session
                   ih/sign-in-as-existing-user 
                   (p/request add-draft-url)
                   (get-in [:response :status])) => 401
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
                 (http-api/post-draft anything) => {:status ::http-api/success
                                                    :result {:_id DRAFT_ID}})
               (let [{response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/add-draft-post :id OBJECTIVE_ID)
                                                         :request-method :post
                                                         :params {:action "submit"
                                                                  :some :content}))]
                 (:headers response) => (ih/location-contains (str "/objectives/" OBJECTIVE_ID "/drafts/" DRAFT_ID))
                 (:status response) => 302))
         
         (fact "posting a draft to an objective that's not in drafting returns a 404 response"
               (against-background
                 (http-api/post-draft anything) => {:status ::http-api/not-found})
               (let [{response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/add-draft-post :id OBJECTIVE_ID)
                                                         :request-method :post
                                                         :params {:action "submit"
                                                                  :some :content}))]
                 (:status response) => 404))))

(facts "about viewing drafts"
      (fact "anyone can view a particular draft"
            (against-background
              (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                        :result {:drafting-started true
                                                                 :_id OBJECTIVE_ID}}
              (http-api/get-draft OBJECTIVE_ID DRAFT_ID) => {:status ::http-api/success
                                                             :result {:_id DRAFT_ID
                                                                      :_created_at "2015-03-24T17:06:37.714Z"
                                                                      :content SOME_HICCUP
                                                                      :objective-id OBJECTIVE_ID
                                                                      :submitter-id USER_ID
                                                                      :username "username"
                                                                      :next-draft-id 4
                                                                      :previous-draft-id 2}})
            (let [{response :response} (p/request user-session (utils/path-for :fe/draft :id OBJECTIVE_ID 
                                                                               :d-id DRAFT_ID))]
              (:status response) => 200
              (:body response) => (contains SOME_HTML)
              (:body response) => (contains (utils/local-path-for :fe/draft :id OBJECTIVE_ID :d-id 2))
              (:body response) => (contains (utils/local-path-for :fe/draft :id OBJECTIVE_ID :d-id 4))))
       
      (fact "anyone can view latest draft"
            (against-background
              (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                        :result {:drafting-started true
                                                                 :_id OBJECTIVE_ID}}
              (http-api/get-draft OBJECTIVE_ID "latest") => {:status ::http-api/success
                                                              :result {:_id DRAFT_ID
                                                                       :_created_at "2015-03-24T17:06:37.714Z"
                                                                       :content SOME_HICCUP
                                                                       :objective-id OBJECTIVE_ID
                                                                       :submitter-id USER_ID}})
            (let [{response :response} (p/request user-session latest-draft-url)]
              (:status response) => 200
              (:body response) => (contains SOME_HTML)))

      (fact "viewing latest draft when drafting hasn't started displays message"
             (against-background
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result {:drafting-started false
                                                                  :_id OBJECTIVE_ID
                                                                  :end-date (utils/string->date-time "2012-12-12")}}
               (http-api/get-draft OBJECTIVE_ID "latest") => {:status ::http-api/forbidden})
            (get-in (p/request user-session latest-draft-url)
                    [:response :body]) => (contains "12-12-2012"))

       (fact "viewing draft list when drafting hasn't started displays message"
             (against-background
               (http-api/get-all-drafts OBJECTIVE_ID) => {:status ::http-api/forbidden}
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result {:end-date (utils/string->date-time "2012-12-12")
                                                                  :drafting-started false}})

             (get-in (p/request user-session draft-list-url)
                     [:response :body]) => (contains "12-12-2012"))

       (fact "anyone can view list of drafts"
             (against-background
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result {:_id OBJECTIVE_ID
                                                                  :end-date (utils/string->date-time "2012-12-12")
                                                                  :drafting-started true}}
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

       (fact "writer can reach add-draft page from a draft page"
             (against-background
               (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
               (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                               :result {:_id USER_ID
                                                                        :username "username"}}
               (http-api/get-user anything) => {:result {:writer-records [{:objective-id OBJECTIVE_ID}]}} 
               (http-api/get-objective OBJECTIVE_ID) => {:status ::http-api/success
                                                         :result {:_id OBJECTIVE_ID
                                                                  :end-date (utils/string->date-time "2012-12-12")
                                                                  :drafting-started true}}
               (http-api/get-draft OBJECTIVE_ID "latest") => {:status ::http-api/success
                                                              :result {:_id DRAFT_ID
                                                                       :_created_at "2015-03-24T17:06:37.714Z"
                                                                       :content SOME_HICCUP
                                                                       :objective-id OBJECTIVE_ID
                                                                       :submitter-id USER_ID}}) 
             (-> user-session
                 ih/sign-in-as-existing-user 
                 (p/request latest-draft-url)
                 (get-in [:response :body])) => (contains (str "/objectives/" OBJECTIVE_ID "/add-draft")))) 

