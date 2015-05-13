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

(facts "about creating profiles"
       (fact "transforms request to profile info"
             (request->profile-info {:params {:name "Name" :biog "Biog"}} USER_ID)
             => {:name "Name" :biog "Biog" :user-uri (str "/users/" USER_ID)}))
