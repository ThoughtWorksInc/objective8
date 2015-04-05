(ns objective8.integration.front-end.index 
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.utils :as utils]
            [objective8.http-api :as http-api]
            [objective8.integration.integration-helpers :as helpers]))

(def untranslated-string-regex #"(?!!DOCTYPE|!IEMobile)!\w+")

(facts "about rendering index page"
       (future-fact "there are no untranslated strings"
             (let [user-session (helpers/test-context)
                   peridot-response-body (-> user-session
                                             (p/request (utils/path-for :fe/index))
                                             :response
                                             :body)]
               (prn peridot-response-body)
               (re-seq untranslated-string-regex peridot-response-body) => empty?)))

(facts "about rendering learn-more page"
       (future-fact "there are no untranslated strings"
             (let [user-session (helpers/test-context)
                   peridot-response-body (-> user-session
                                             (p/request (utils/path-for :fe/learn-more))
                                             :response
                                             :body)]
               (prn peridot-response-body)
               (re-seq untranslated-string-regex peridot-response-body) => empty?)))  

(facts "about rendering project-status page"
       (future-fact "there are no untranslated strings"
             (let [user-session (helpers/test-context)
                   peridot-response-body (-> user-session
                                             (p/request (utils/path-for :fe/project-status))
                                             :response
                                             :body)]
               (prn peridot-response-body)
               (re-seq untranslated-string-regex peridot-response-body) => empty?)))  

(def drafting-objective {:title "my objective title"
                         :goal-1 "my objective goal"
                         :description "my objective description"
                         :end-date (utils/string->date-time "2012-12-12")
                         :uri (str "/objectives/" 5)
                         :status "drafting"})

(def open-objective {assoc drafting-objective :status "open" 
                     :end-date (utils/date-time->date-time-plus-30-days (utils/current-time))})

(facts "about rendering objective-list page"
       (future-fact "there are no untranslated strings"
             (against-background
               (http-api/get-all-objectives) => {:status ::http-api/success 
                                                 :result [drafting-objective open-objective]})
             (let [user-session (helpers/test-context)
                   peridot-response-body (-> user-session
                                             (p/request (utils/path-for :fe/objective-list))
                                             :response
                                             :body)]
               (prn peridot-response-body)
               (re-seq untranslated-string-regex peridot-response-body) => empty?)))  

(facts "about rendering create-objective page"
       (future-fact "there are no untranslated strings"
             (against-background
               (oauth/access-token anything anything anything) => {:user_id "TWITTER_ID"}
               (http-api/find-user-by-twitter-id anything) => {:status ::http-api/success
                                                               :result {:_id 10
                                                                        :username "username"}})
             (let [user-session (helpers/test-context)
                   {status :status body :body} (-> user-session
                            (helpers/sign-in-as-existing-user)
                            (p/request (utils/path-for :fe/create-objective-form))
                            :response)]
               (prn body)
               status => 200
               (re-seq untranslated-string-regex body) => empty?)))  
