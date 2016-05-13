(ns objective8.unit.front-end-requests-test
  (:require [midje.sweet :refer :all]
            [clojure.string :as s]
            [endophile.core :as ec]
            [endophile.hiccup :as eh]
            [objective8.front-end.front-end-requests :refer :all]
            [objective8.utils :as utils]))

(def USER_ID 1)
(def OBJECTIVE_ID 2)
(def QUESTION_ID 3)

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
             (valid-email? "ab@c d") => falsey)

       (fact "they have max length 256 char"
             (valid-email? (str (string-of-length 100) "@" (string-of-length 155))) => truthy
             (valid-email? (str (string-of-length 100) "@" (string-of-length 156))) => falsey))

(facts "about valid usernames"
       (fact "they contain between 1 and 16 chracters"
             (valid-username? "username") => truthy
             (valid-username? "") => falsey
             (valid-username? (string-of-length 17)) => falsey)

       (fact "must be alphanumeric"
             (valid-username? "ABCabc123") => truthy
             (valid-username? "ABC abc123") => falsey
             (valid-username? "ABC-abc123") => falsey))

(facts "about transforming requests to user-sign-up-data"
       (tabular
         (fact "extracts the relevant data"
               (let [user-sign-up-data (request->user-sign-up-data {:params  {:username      ?username
                                                                              :email-address ?email-address}
                                                                    :session {:auth-provider-user-email ?auth-email}})]

                 (:data user-sign-up-data) => {:username      ?username
                                               :email-address (or ?email-address ?auth-email)}
                 (:status user-sign-up-data) => ::objective8.front-end.front-end-requests/valid
                 (:report user-sign-up-data) => {}))

         ?username          ?email-address           ?auth-email
         "validUsername"    "abc@def.com"            nil
         "validUsername"    nil                      "abc@def.com"
         "validUsername"    "user-entered@email.com" "auth@email.com")


       (fact "reports validation errors correctly when parameters are missing"
             (let [user-sign-up-data (request->user-sign-up-data {:params {}})]
               (:data user-sign-up-data) => {:username "" :email-address ""}
               (:status user-sign-up-data) => ::objective8.front-end.front-end-requests/invalid
               (:report user-sign-up-data) => {:username      #{:invalid}
                                               :email-address #{:empty}}))

       (tabular
         (fact "reports validation errors from a request"
               (let [user-sign-up-data (request->user-sign-up-data {:params {:username      ?username
                                                                             :email-address ?email-address}})]
                 (:data user-sign-up-data) => {:username      (s/trim ?username)
                                               :email-address (s/trim ?email-address)}
                 (:status user-sign-up-data) => ::objective8.front-end.front-end-requests/invalid
                 (:report user-sign-up-data) => ?report))
         ?username        ?email-address          ?report
         ""               "abc@def.com"           {:username #{:invalid}}
         " "              "abc@def.com"           {:username #{:invalid}}
         "validUsername"  ""                      {:email-address #{:empty}}
         "validUsername"  " "                     {:email-address #{:empty}}
         "validUsername"  "no-at-symbol"          {:email-address #{:invalid}}))

(facts "about transforming requests to objective-data"
       (fact "creates an objective from a request"
             (let [test-objective {:title "My Objective" :description "I like cake"}
                   objective-data (request->objective-data {:params test-objective} USER_ID)]
               (:data objective-data) => (assoc test-objective :created-by-id USER_ID)
               (:status objective-data) => ::objective8.front-end.front-end-requests/valid
               (:report objective-data) => {}))

       (fact "reports validation errors correctly when parameters are missing"
             (let [objective-data (request->objective-data {:params {}} USER_ID)]
               (:data objective-data) => {:title         ""
                                          :description   ""
                                          :created-by-id USER_ID}
               (:status objective-data) => ::objective8.front-end.front-end-requests/invalid
               (:report objective-data) => {:title       #{:length}
                                            :description #{:empty}}))

       (tabular
         (fact "reports validation errors"
               (let [invalid-test-objective {:title ?title :description ?description}
                     objective-data (request->objective-data {:params invalid-test-objective} USER_ID)]
                 (:data objective-data) => {:title         (s/trim ?title)
                                            :description   (s/trim ?description)
                                            :created-by-id USER_ID}
                 (:status objective-data) => ::objective8.front-end.front-end-requests/invalid
                 (:report objective-data) => ?report))
         ?title ?description ?report
         "" "I like CAKE!" {:title #{:length}}
         "      " "I like CAKE!" {:title #{:length}}
         "Hi" "I like CAKE!" {:title #{:length}}
         (string-of-length 121) "I like CAKE!" {:title #{:length}}
         "Valid Title" "" {:description #{:empty}}
         "Valid Title" "       " {:description #{:empty}}
         "Valid Title" (string-of-length 5001) {:description #{:length}}))

(facts "about transforming requests to question-data"
       (fact "extracts the relevant data"
             (let [question-data (request->question-data {:route-params {:id (str OBJECTIVE_ID)}
                                                          :params       {:question "The question"}}
                                                         USER_ID)]
               (:data question-data) => {:question      "The question"
                                         :created-by-id USER_ID
                                         :objective-id  OBJECTIVE_ID}
               (:status question-data) => ::objective8.front-end.front-end-requests/valid
               (:report question-data) => {}))

       (fact "reports validation errors correctly when parameters are missing"
             (let [question-data (request->question-data {:route-params {:id (str OBJECTIVE_ID)}
                                                          :params       {}}
                                                         USER_ID)]
               (:data question-data) => {:question      ""
                                         :created-by-id USER_ID
                                         :objective-id  OBJECTIVE_ID}
               (:status question-data) => ::objective8.front-end.front-end-requests/invalid
               (:report question-data) => {:question #{:length}}))

       (fact "reports validation errors"
             (let [question-data (request->question-data {:route-params {:id (str OBJECTIVE_ID)}
                                                          :params       {:question "Why?"}}
                                                         USER_ID)]
               (:data question-data) => {:question      "Why?"
                                         :created-by-id USER_ID
                                         :objective-id  OBJECTIVE_ID}
               (:status question-data) => ::objective8.front-end.front-end-requests/invalid
               (:report question-data) => {:question #{:length}})))

(fact "about transforming requests to answer-data"
      (fact "extracts the relevant data"
            (let [answer-data (request->answer-data {:route-params {:id   (str OBJECTIVE_ID)
                                                                    :q-id (str QUESTION_ID)}
                                                     :params       {:answer "the answer"}}
                                                    USER_ID)]
              (:data answer-data) => {:answer        "the answer"
                                      :question-id   QUESTION_ID
                                      :objective-id  OBJECTIVE_ID
                                      :created-by-id USER_ID}
              (:status answer-data) => ::objective8.front-end.front-end-requests/valid
              (:report answer-data) => {}))

      (fact "reports validation errors correctly when parameters are missing"
            (let [answer-data (request->answer-data {:route-params {:id   (str OBJECTIVE_ID)
                                                                    :q-id (str QUESTION_ID)}
                                                     :params       {}}
                                                    USER_ID)]
              (:data answer-data) => {:answer        ""
                                      :question-id   QUESTION_ID
                                      :objective-id  OBJECTIVE_ID
                                      :created-by-id USER_ID}
              (:status answer-data) => ::objective8.front-end.front-end-requests/invalid
              (:report answer-data) => {:answer #{:empty}}))

      (tabular
        (fact "reports validation errors"
              (let [answer-data (request->answer-data {:route-params {:id   (str OBJECTIVE_ID)
                                                                      :q-id (str QUESTION_ID)}
                                                       :params       {:answer ?answer}}
                                                      USER_ID)]
                (:data answer-data) => {:answer        (s/trim ?answer)
                                        :question-id   QUESTION_ID
                                        :objective-id  OBJECTIVE_ID
                                        :created-by-id USER_ID}
                (:status answer-data) => ::objective8.front-end.front-end-requests/invalid
                (:report answer-data) => ?report))
        ?answer ?report
        "" {:answer #{:empty}}
        " " {:answer #{:empty}}
        (string-of-length 501) {:answer #{:length}}))

(fact "about transforming requests to comment-data"
      (fact "extracts the relevant data"
            (let [comment-data (request->comment-data {:params {:comment        "the comment"
                                                                :comment-on-uri "/some/uri"}}
                                                      USER_ID)]
              (:data comment-data) => {:comment        "the comment"
                                       :comment-on-uri "/some/uri"
                                       :created-by-id  USER_ID}
              (:status comment-data) => ::objective8.front-end.front-end-requests/valid
              (:report comment-data) => {}))

      (fact "reports validation errors correctly when parameters are missing"
            (request->comment-data {:params {}} USER_ID) => nil

            (let [comment-data (request->comment-data {:params {:comment-on-uri "/some/uri"}} USER_ID)]
              (:data comment-data) => {:comment        ""
                                       :comment-on-uri "/some/uri"
                                       :created-by-id  USER_ID}
              (:status comment-data) => ::objective8.front-end.front-end-requests/invalid
              (:report comment-data) => {:comment #{:empty}}))

      (tabular
        (fact "reports validation errors"
              (let [comment-data (request->comment-data {:params {:comment        ?comment
                                                                  :comment-on-uri "/some/uri"}}
                                                        USER_ID)]
                (:data comment-data) => {:comment        (s/trim ?comment)
                                         :comment-on-uri "/some/uri"
                                         :created-by-id  USER_ID}
                (:status comment-data) => ::objective8.front-end.front-end-requests/invalid
                (:report comment-data) => {:comment ?error-type}))
        ?comment ?error-type
        "" #{:empty}
        " " #{:empty}
        (string-of-length 501) #{:length}))

(facts "about transforming requests to annotation data"
       (tabular
         (fact "extracts the relevant data"
               (let [annotation-data (request->annotation-data {:params {:reason         ?reason
                                                                         :comment        "comment"
                                                                         :comment-on-uri "some-uri"}} USER_ID)]
                 (:data annotation-data) => {:reason         ?reason
                                             :comment        "comment"
                                             :comment-on-uri "some-uri"
                                             :created-by-id  USER_ID}
                 (:status annotation-data) => ::objective8.front-end.front-end-requests/valid
                 (:report annotation-data) => {}))
         ?reason "general" "unclear" "expand" "suggestion" "language")

       (fact "reports validation errors correctly when parameters are missing"
             (request->annotation-data {:params {}} USER_ID) => nil

             (let [annotation-data (request->annotation-data {:params {:comment-on-uri "some-uri"}} USER_ID)]
               (:data annotation-data) => {:reason         ""
                                           :comment        ""
                                           :comment-on-uri "some-uri"
                                           :created-by-id  USER_ID}
               (:status annotation-data) => ::objective8.front-end.front-end-requests/invalid
               (:report annotation-data) => {:reason  #{:incorrect-type}
                                             :comment #{:empty}}))
       (tabular
         (fact "reports validation errors"
               (let [annotation-data (request->annotation-data {:params {:reason         ?reason
                                                                         :comment        ?comment
                                                                         :comment-on-uri "some-uri"}} USER_ID)]
                 (:data annotation-data) => {:reason         ?reason
                                             :comment        (s/trim ?comment)
                                             :comment-on-uri "some-uri"
                                             :created-by-id  USER_ID}
                 (:status annotation-data) => ::objective8.front-end.front-end-requests/invalid
                 (:report annotation-data) => ?report))
         ?reason ?comment ?report
         "" "comment" {:reason #{:incorrect-type}}
         "invalid-reason" "comment" {:reason #{:incorrect-type}}
         "general" "" {:comment #{:empty}}
         "general" " " {:comment #{:empty}}
         "general" (string-of-length 501) {:comment #{:length}}))

(facts "about transforming requests to writer invitation data"
       (fact "extracts the relevant data"
             (let [invitation-data (request->invitation-data {:params       {:writer-name  "Jenny"
                                                                             :reason       "Just because"
                                                                             :writer-email "writer@email.com"}
                                                              :route-params {:id (str OBJECTIVE_ID)}}
                                                             USER_ID)]
               (:data invitation-data) => {:writer-name   "Jenny"
                                           :reason        "Just because"
                                           :writer-email  "writer@email.com"
                                           :objective-id  OBJECTIVE_ID
                                           :invited-by-id USER_ID}
               (:status invitation-data) => ::objective8.front-end.front-end-requests/valid
               (:report invitation-data) => {}))

       (fact "reports validation errors when parameters are missing"
             (let [invitation-data (request->invitation-data {:params       {}
                                                              :route-params {:id (str OBJECTIVE_ID)}}
                                                             USER_ID)]
               (:data invitation-data) => {:writer-name   ""
                                           :reason        ""
                                           :writer-email  ""
                                           :objective-id  OBJECTIVE_ID
                                           :invited-by-id USER_ID}
               (:status invitation-data) => ::objective8.front-end.front-end-requests/invalid
               (:report invitation-data) => {:writer-name  #{:empty}
                                             :reason       #{:empty}
                                             :writer-email #{:empty}}))

       (tabular
         (fact "reports validation errors"
               (let [invitation-data (request->invitation-data {:params       {:writer-name  ?writer-name
                                                                               :reason       ?reason
                                                                               :writer-email ?writer-email}
                                                                :route-params {:id (str OBJECTIVE_ID)}}
                                                               USER_ID)]
                 (:data invitation-data) => {:writer-name   (s/trim ?writer-name)
                                             :reason        (s/trim ?reason)
                                             :writer-email  (s/trim ?writer-email)
                                             :objective-id  OBJECTIVE_ID
                                             :invited-by-id USER_ID}
                 (:status invitation-data) => ::objective8.front-end.front-end-requests/invalid
                 (:report invitation-data) => ?report))
         ?writer-name ?reason ?writer-email ?report
         "" "a reason" "a@b.com" {:writer-name #{:empty}}
         "    " "a reason" "a@b.com" {:writer-name #{:empty}}
         (string-of-length 51) "a reason" "a@b.com" {:writer-name #{:length}}
         "Jenny" "" "a@b.com" {:reason #{:empty}}
         "Jenny" (string-of-length 5001) "a@b.com" {:reason #{:length}}
         "Jenny" "a reason" "" {:writer-email #{:empty}}
         "Jenny" "a reason" "a" {:writer-email #{:invalid}}
         "" " " "" {:writer-name  #{:empty}
                    :reason       #{:empty}
                    :writer-email #{:empty}}))

(facts "about transforming requests to writer note data"
       (fact "extracts the relevant data"
             (let [note-data (request->writer-note-data {:params {:note        "the note"
                                                                  :note-on-uri "/some/uri"}}
                                                        USER_ID)]
               (:data note-data) => {:note          "the note"
                                     :note-on-uri   "/some/uri"
                                     :created-by-id USER_ID}
               (:status note-data) => ::objective8.front-end.front-end-requests/valid
               (:report note-data) => {}))

       (fact "reports validation errors when parameters are missing"
             (request->writer-note-data {:params {}} USER_ID) => nil

             (let [note-data (request->writer-note-data {:params {:note-on-uri "/some/uri"}}
                                                        USER_ID)]
               (:data note-data) => {:note          ""
                                     :note-on-uri   "/some/uri"
                                     :created-by-id USER_ID}
               (:status note-data) => ::objective8.front-end.front-end-requests/invalid
               (:report note-data) => {:note #{:empty}}))

       (tabular
         (fact "reports validation errors"
               (let [note-data (request->writer-note-data {:params {:note        ?note
                                                                    :note-on-uri "/some/uri"}}
                                                          USER_ID)]
                 (:data note-data) => {:note          ?note
                                       :note-on-uri   "/some/uri"
                                       :created-by-id USER_ID}
                 (:status note-data) => ::objective8.front-end.front-end-requests/invalid
                 (:report note-data) => ?report))
         ?note ?report
         "" {:note #{:empty}}
         (string-of-length 501) {:note #{:length}}))

(facts "about transforming requests to profile data"
       (fact "extracts the relevant data"
             (let [profile-data (request->profile-data {:params {:name "Name" :biog "Biog"}} USER_ID)]
               (:data profile-data) => {:name     "Name"
                                        :biog     "Biog"
                                        :user-uri (str "/users/" USER_ID)}
               (:status profile-data) => ::objective8.front-end.front-end-requests/valid
               (:report profile-data) => {}))

       (fact "reports validation errors when parameters are missing"
             (let [profile-data (request->profile-data {:params {}} USER_ID)]
               (:data profile-data) => {:name     ""
                                        :biog     ""
                                        :user-uri (str "/users/" USER_ID)}
               (:status profile-data) => ::objective8.front-end.front-end-requests/invalid
               (:report profile-data) => {:name #{:empty} :biog #{:empty}}))

       (tabular
         (fact "reports validation errors"
               (let [profile-data (request->profile-data {:params {:name ?name :biog ?biog}} USER_ID)]
                 (:data profile-data) => {:name     (s/trim ?name)
                                          :biog     (s/trim ?biog)
                                          :user-uri (str "/users/" USER_ID)}
                 (:status profile-data) => ::objective8.front-end.front-end-requests/invalid
                 (:report profile-data) => ?report))
         ?name ?biog ?report
         "" "biography" {:name #{:empty}}
         "    " "biography" {:name #{:empty}}
         (string-of-length 51) "biography" {:name #{:length}}
         "Jenny" "" {:biog #{:empty}}
         "Jenny" (string-of-length 5001) {:biog #{:length}}
         "" " " {:name #{:empty}
                 :biog #{:empty}}))

(fact "about transforming requests to imported draft data"
      (fact "extracts the relevant data"
            (let [html-string "<p>Hello!</p>"
                  imported-draft-data (request->imported-draft-data {:route-params {:id (str OBJECTIVE_ID)}
                                                                     :params       {:google-doc-html-content html-string}} USER_ID)]
              (:data imported-draft-data) => {:submitter-id USER_ID
                                              :objective-id OBJECTIVE_ID
                                              :content      (utils/html->hiccup html-string)}
              (:status imported-draft-data) => ::objective8.front-end.front-end-requests/valid
              (:report imported-draft-data) => {}))

      (fact "reports validation errors when params are missing"
            (let [imported-draft-data (request->imported-draft-data {:route-params {:id (str OBJECTIVE_ID)}
                                                                     :params       {}} USER_ID)]
              (:data imported-draft-data) => {:submitter-id USER_ID
                                              :objective-id OBJECTIVE_ID
                                              :content      (utils/html->hiccup "")}
              (:status imported-draft-data) => ::objective8.front-end.front-end-requests/invalid
              (:report imported-draft-data) => {:content #{:empty}}))

      (fact "reports validation errors"
            (let [html-string ""
                  imported-draft-data (request->imported-draft-data {:route-params {:id (str OBJECTIVE_ID)}
                                                                     :params       {:google-doc-html-content html-string}} USER_ID)]
              (:data imported-draft-data) => {:submitter-id USER_ID
                                              :objective-id OBJECTIVE_ID
                                              :content      (utils/html->hiccup html-string)}
              (:status imported-draft-data) => ::objective8.front-end.front-end-requests/invalid
              (:report imported-draft-data) => {:content #{:empty}})))

(def SOME_MARKDOWN "A heading\n===\nSome content")
(def SOME_HICCUP (eh/to-hiccup (ec/mp SOME_MARKDOWN)))

(fact "about transforming requests to add-draft-data"
      (fact "extracts the relevant data"
            (let [add-draft-data (request->add-draft-data {:route-params {:id (str OBJECTIVE_ID)}
                                                           :params       {:content SOME_MARKDOWN}}
                                                          USER_ID)]
              (:data add-draft-data) => {:submitter-id USER_ID
                                         :objective-id OBJECTIVE_ID
                                         :markdown     SOME_MARKDOWN
                                         :hiccup       SOME_HICCUP}
              (:status add-draft-data) => ::objective8.front-end.front-end-requests/valid
              (:report add-draft-data) => {}))

      (tabular
        (fact "reports validation errors"
              (let [add-draft-data (request->add-draft-data {:route-params {:id (str OBJECTIVE_ID)}
                                                             :params       ?params}
                                                            USER_ID)]
                (:data add-draft-data) => {:submitter-id USER_ID
                                           :objective-id OBJECTIVE_ID
                                           :markdown     ?expected-markdown
                                           :hiccup       '()}
                (:status add-draft-data) => ::objective8.front-end.front-end-requests/invalid
                (:report add-draft-data) => ?report))
        ?params ?expected-markdown ?report
        {:content ""} "" {:markdown #{:empty}}
        {:content " "} "" {:markdown #{:empty}}
        {} "" {:markdown #{:empty}}))

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
             (request->down-vote-info {:params {}} USER_ID) => nil))

(facts "about starring objectives"
       (fact "transform request to star info"
             (request->star-info {:params {:objective-uri "/some/uri"}} USER_ID)
             => {:objective-uri "/some/uri" :created-by-id USER_ID})

       (fact "returns nil when required star info is not provided"
             (request->star-info {:params {}} USER_ID) => nil))

(facts "about marking questions"
       (fact "Validates the request and returns the data required for marking a question"
             (request->mark-info {:params {:question-uri "/some/uri"}} USER_ID) => {:question-uri   "/some/uri"
                                                                                    :created-by-uri (str "/users/" USER_ID)})

       (fact "returns nil when required mark-info is not provided"
             (request->mark-info {:params {}} USER_ID) => nil))

(facts "about promoting objectives"
       (fact "transforms request into promoted objective data"
             (request->promoted-data {:params {:objective-uri "/objective/123456"}} USER_ID)
             => {:objective-uri"/objective/123456" :promoted-by (str "/users/" USER_ID)}))
