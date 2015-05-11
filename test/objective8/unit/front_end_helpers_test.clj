(ns objective8.unit.front-end-helpers-test
  (:require [midje.sweet :refer :all]
            [objective8.front-end-helpers :refer :all]
            [objective8.utils :as utils]))


(def USER_ID 1)
(def OBJECTIVE_ID 2)
(def QUESTION_ID 3)
(def GLOBAL_ID 5)

(def date-time (utils/string->date-time "2015-01-03"))

(fact "creates draft-info map from a request"
      (let [html-string "<p>Hello!</p>"
            draft-info (request->draft-info {:params {:id (str OBJECTIVE_ID)
                                                       :google-doc-html-content html-string}} USER_ID)]
        draft-info => {:objective-id OBJECTIVE_ID 
                   :submitter-id USER_ID 
                   :content  (utils/html->hiccup html-string)}))

(facts "about up voting"
       (fact "transforms request to up vote info"
             (request->up-vote-info {:params {:vote-on-uri "/some/uri"}} USER_ID)
             => {:vote-on-uri "/some/uri" :created-by-id USER_ID :vote-type "up"})

       (fact "returns nil when required up-vote information not provided"
             (request->up-vote-info {:params {}} USER_ID) => nil))

(facts "about down voting"
       (fact "transforms request to down vote info"
             (request->down-vote-info {:params {:vote-on-uri "/some/uri"}} USER_ID)
             => {:vote-on-uri "/some/uri" :created-by-id USER_ID :vote-type "down"})

       (fact "returns nil when required down-vote information not provided"
             (request->down-vote-info {:params {}} USER_ID)
             => nil))

(facts "about starring objectives"
       (fact "transform request to star info"
             (request->star-info {:params {:objective-uri "/some/uri"}} USER_ID)
             => {:objective-uri "/some/uri" :created-by-id USER_ID})

       (fact "returns nil when required star info is not provided"
             (request->star-info {:params {}} USER_ID)
             => nil)) 

(facts "about marking questions"
       (fact "Validates the request and returns the data required for marking a question"
             (request->mark-info {:params {:question-uri "/some/uri"}} USER_ID) => {:question-uri "/some/uri"
                                                                                    :created-by-uri (str "/users/" USER_ID)}))

(facts "about creating profiles"
       (fact "transforms request to profile info"
             (request->profile-info {:params {:name "Name" :biog "Biog"}} USER_ID)
             => {:name "Name" :biog "Biog" :user-uri (str "/users/" USER_ID)}))
