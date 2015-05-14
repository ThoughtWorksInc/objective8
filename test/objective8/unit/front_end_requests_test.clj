(ns objective8.unit.front-end-requests-test
  (:require [midje.sweet :refer :all]
            [endophile.core :as ec]
            [endophile.hiccup :as eh]
            [objective8.front-end-requests :refer :all]
            [objective8.utils :as utils]))

(def USER_ID 1)
(def OBJECTIVE_ID 2)
(def QUESTION_ID 3)

(def test-objective {:title "My Objective"
                     :description "I like cake"})

(def invalid-test-objective {:title ""
                             :description "I like cake"})

(def date (utils/string->date-time "2015-01-01"))
(def date-plus-30-days (utils/string->date-time "2015-01-31"))

(defn string-of-length [l]
  (apply str (repeat l "x")))

(facts "about valid email addresses"
       (fact "they contain exactly one @ symbol"
             (valid-email? "a@b") => truthy
             (valid-email? "a@b.com") => truthy
             (valid-email? "12345") => falsey
             (valid-email? "a@b@c") => falsey)
       
       (fact "the @ symbol is not in the first or last position"
             (valid-email? "@abc") => falsey
             (valid-email? "abc@") => falsey)
       
       (fact "they contains no spaces"
             (valid-email? "a b@cd") => falsey
             (valid-email? "ab@c d") => falsey))

(facts "about transforming requests to objective-data"
       (fact "creates an objective from a request"
             (let [objective-data (request->objective-data {:params test-objective} USER_ID date)]
               (:data objective-data) => (assoc test-objective :created-by-id USER_ID :end-date date-plus-30-days)
               (:status objective-data) => ::objective8.front-end-requests/valid
               (:report objective-data) => {}))

       (fact "reports validation errors from a request"
             (let [objective-data (request->objective-data {:params invalid-test-objective} USER_ID date)]
               (:data objective-data) => (assoc invalid-test-objective :created-by-id USER_ID :end-date date-plus-30-days)
               (:status objective-data) => ::objective8.front-end-requests/invalid
               (:report objective-data) => {:title #{:length}})))

(facts "about transforming requests to question-data"
       (fact "extracts the relevant data"
             (let [question-data (request->question-data {:route-params {:id (str OBJECTIVE_ID)}
                                                          :params {:question "The question"}}
                                                         USER_ID)]
               (:data question-data) => {:question "The question"
                                         :created-by-id USER_ID
                                         :objective-id OBJECTIVE_ID}
               (:status question-data) => ::objective8.front-end-requests/valid
               (:report question-data) => {}))
       
       (fact "reports validation errors"
             (let [question-data (request->question-data {:route-params {:id (str OBJECTIVE_ID)}
                                                          :params {:question "Why?"}}
                                                         USER_ID)]
               (:data question-data) => {:question "Why?"
                                         :created-by-id USER_ID
                                         :objective-id OBJECTIVE_ID}
               (:status question-data) => ::objective8.front-end-requests/invalid
               (:report question-data) => {:question #{:length}})))

(fact "about transforming requests to answer-data"
      (fact "extracts the relevant data"
            (let [answer-data (request->answer-data {:route-params {:id (str OBJECTIVE_ID)
                                                                    :q-id (str QUESTION_ID)}
                                                     :params {:answer "the answer"}}
                                                    USER_ID)]
              (:data answer-data) => {:answer "the answer"
                                      :question-id QUESTION_ID
                                      :objective-id OBJECTIVE_ID
                                      :created-by-id USER_ID}
              (:status answer-data) => ::objective8.front-end-requests/valid
              (:report answer-data) => {}))

      (tabular
       (fact "reports validation errors"
             (let [answer-data (request->answer-data {:route-params {:id (str OBJECTIVE_ID)
                                                                     :q-id (str QUESTION_ID)}
                                                      :params {:answer ""}}
                                                     USER_ID)]
               (:data answer-data) => {:answer ""
                                       :question-id QUESTION_ID
                                       :objective-id OBJECTIVE_ID
                                       :created-by-id USER_ID}
               (:status answer-data) => ::objective8.front-end-requests/invalid
               (:report answer-data) => {:answer #{:empty}}))
       ?answer                 ?report
       ""                      {:answer #{:empty}}
       (string-of-length 501)  {:answer #{:length}}))

(fact "about transforming requests to comment-data"
      (fact "extracts the relevant data"
            (let [comment-data (request->comment-data {:params {:comment "the comment"
                                                                :comment-on-uri "/some/uri"}}
                                                      USER_ID)]
              (:data comment-data) => {:comment "the comment"
                                       :comment-on-uri "/some/uri"
                                       :created-by-id USER_ID}
              (:status comment-data) => ::objective8.front-end-requests/valid
              (:report comment-data) => {}))

      (tabular
       (fact "reports validation errors"
             (let [comment-data (request->comment-data {:params {:comment ?comment
                                                                 :comment-on-uri "/some/uri"}}
                                                       USER_ID)]
               (:data comment-data) => {:comment ?comment
                                        :comment-on-uri "/some/uri"
                                        :created-by-id USER_ID}
               (:status comment-data) => ::objective8.front-end-requests/invalid
               (:report comment-data) => {:comment ?error-type}))
       ?comment                 ?error-type
       ""                       #{:empty}
       (string-of-length 501)   #{:length})

      (fact "returns nil when data that is not directly provided by the user is invalid"
            (let [comment-data (request->comment-data {:params {:comment ""}}
                                                      USER_ID)]
              comment-data => nil)))

(facts "about transforming requests to writer invitation data"
       (fact "extracts the relevant data"
             (let [invitation-data (request->invitation-data {:params {:writer-name "Jenny"
                                                                       :reason "Just because"
                                                                       :writer-email "writer@email.com"}
                                                              :route-params {:id (str OBJECTIVE_ID)}}
                                                             USER_ID)]
               (:data invitation-data) => {:writer-name "Jenny"
                                           :reason "Just because"
                                           :writer-email "writer@email.com"
                                           :objective-id OBJECTIVE_ID
                                           :invited-by-id USER_ID}
               (:status invitation-data) => ::objective8.front-end-requests/valid
               (:report invitation-data) => {}))

       (tabular
        (fact "reports validation errors"
              (let [invitation-data (request->invitation-data {:params {:writer-name ?writer-name
                                                                        :reason ?reason
                                                                        :writer-email ?writer-email}
                                                               :route-params {:id (str OBJECTIVE_ID)}}
                                                              USER_ID)]
                (:data invitation-data) => {:writer-name (clojure.string/trim ?writer-name)
                                            :reason (clojure.string/trim ?reason)
                                            :writer-email (clojure.string/trim ?writer-email)
                                            :objective-id OBJECTIVE_ID
                                            :invited-by-id USER_ID}
                (:status invitation-data) => ::objective8.front-end-requests/invalid
                (:report invitation-data) => ?report))
        ?writer-name           ?reason                 ?writer-email    ?report
        ""                     "a reason"              "a@b.com"        {:writer-name #{:empty}}
        "    "                 "a reason"              "a@b.com"        {:writer-name #{:empty}}
        (string-of-length 51)  "a reason"              "a@b.com"        {:writer-name #{:length}}
        "Jenny"                ""                      "a@b.com"        {:reason #{:empty}}
        "Jenny"                (string-of-length 5001) "a@b.com"        {:reason #{:length}}
        "Jenny"                "a reason"              ""               {:writer-email #{:empty}}
        "Jenny"                "a reason"              "a"              {:writer-email #{:invalid}}
        ""                     " "                     ""               {:writer-name #{:empty}
                                                                         :reason #{:empty}
                                                                         :writer-email #{:empty}})

       (fact "returns nil when data that is not directly provided by the user is invalid"
             (let [invitation-data (request->invitation-data {:params {:writer-name "Jenny"
                                                                       :reason "a reason"
                                                                       :writer-email "a@b"
                                                                       :objective-id nil}}
                                                              USER_ID)]
               invitation-data => nil)))

(facts "about transforming requests to writer note data"
       (fact "extracts the relevant data"
             (let [note-data (request->writer-note-data {:params {:note "the note"
                                                                  :note-on-uri "/some/uri"}}
                                                        USER_ID)]
               (:data note-data) => {:note "the note"
                                     :note-on-uri "/some/uri"
                                     :created-by-id USER_ID}
               (:status note-data) => ::objective8.front-end-requests/valid
               (:report note-data) => {}))

       (fact "reports validation errors"
             (let [note-data (request->writer-note-data {:params {:note ""
                                                                  :note-on-uri "/some/uri"}}
                                                        USER_ID)]
               (:data note-data) => {:note ""
                                     :note-on-uri "/some/uri"
                                     :created-by-id USER_ID}
               (:status note-data) => ::objective8.front-end-requests/invalid
               (:report note-data) => {:note #{:empty}}))

       (fact "returns nil when data that is not directly provided by the user is invalid"
             (let [note-data (request->writer-note-data {:params {:note "the note"}}
                                                        USER_ID)]
               note-data => nil)))

(facts "about transforming requests to profile data"
       (fact "extracts the relevant data"
             (let [profile-data (request->profile-data {:params {:name "Name" :biog "Biog"}} USER_ID)]
               (:data profile-data) => {:name "Name" 
                                        :biog "Biog" 
                                        :user-uri (str "/users/" USER_ID)}
               (:status profile-data) => ::objective8.front-end-requests/valid
               (:report profile-data) => {}))
       
       (tabular
        (fact "reports validation errors"
              (let [profile-data (request->profile-data {:params {:name ?name :biog ?biog}} USER_ID)]
                (:data profile-data) => {:name (clojure.string/trim ?name)
                                         :biog (clojure.string/trim ?biog)
                                         :user-uri (str "/users/" USER_ID)}
                (:status profile-data) => ::objective8.front-end-requests/invalid
                (:report profile-data) => ?report))
        ?name                  ?biog                   ?report
        ""                     "biography"             {:name #{:empty}}
        "    "                 "biography"             {:name #{:empty}}
        (string-of-length 51)  "biography"             {:name #{:length}}
        "Jenny"                ""                      {:biog #{:empty}}
        "Jenny"                (string-of-length 5001) {:biog #{:length}}
        ""                     " "                     {:name #{:empty}
                                                        :biog #{:empty}})) 

(fact "about transforming requests to imported draft data"
      (fact "extracts the relevant data"
            (let [html-string "<p>Hello!</p>"
                  imported-draft-data (request->imported-draft-data {:route-params {:id (str OBJECTIVE_ID)}
                                                                     :params {:google-doc-html-content html-string}} USER_ID)]
              (:data imported-draft-data) => {:submitter-id USER_ID
                                              :objective-id OBJECTIVE_ID
                                              :content (utils/html->hiccup html-string)}
              (:status imported-draft-data) => ::objective8.front-end-requests/valid
              (:report imported-draft-data) => {}))
      
      (fact "reports validation errors"
            (let [html-string ""
                  imported-draft-data (request->imported-draft-data {:route-params {:id (str OBJECTIVE_ID)}
                                                                     :params {:google-doc-html-content html-string}} USER_ID)]
              (:data imported-draft-data) => {:submitter-id USER_ID
                                              :objective-id OBJECTIVE_ID
                                              :content (utils/html->hiccup html-string)}
              (:status imported-draft-data) => ::objective8.front-end-requests/invalid
              (:report imported-draft-data) => {:content #{:empty}})))

(def SOME_MARKDOWN  "A heading\n===\nSome content")
(def SOME_HICCUP (eh/to-hiccup (ec/mp SOME_MARKDOWN)))

(fact "about transforming requests to add-draft-data"
      (fact "extracts the relevant data"
            (let [add-draft-data (request->add-draft-data {:route-params {:id (str OBJECTIVE_ID)}
                                                           :params {:content SOME_MARKDOWN}}
                                                          USER_ID)]
              (:data add-draft-data) => {:submitter-id USER_ID
                                         :objective-id OBJECTIVE_ID
                                         :markdown SOME_MARKDOWN
                                         :hiccup SOME_HICCUP}
              (:status add-draft-data) => ::objective8.front-end-requests/valid
              (:report add-draft-data) => {}))

      (tabular
       (fact "reports validation errors"
             (let [add-draft-data (request->add-draft-data {:route-params {:id (str OBJECTIVE_ID)}
                                                            :params ?params}
                                                           USER_ID)]
               (:data add-draft-data) => {:submitter-id USER_ID
                                          :objective-id OBJECTIVE_ID
                                          :markdown ?expected-markdown
                                          :hiccup '()}
               (:status add-draft-data) => ::objective8.front-end-requests/invalid
               (:report add-draft-data) => ?report))
       ?params        ?expected-markdown   ?report
       {:content ""}  ""                   {:markdown #{:empty}}
       {:content " "} ""                   {:markdown #{:empty}}
       {}             ""                   {:markdown #{:empty}}))

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


