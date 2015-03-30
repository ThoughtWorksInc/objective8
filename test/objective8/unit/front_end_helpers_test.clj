(ns objective8.unit.front-end-helpers-test
  (:require [midje.sweet :refer :all]
            [objective8.front-end-helpers :refer :all]
            [objective8.utils :as utils]))


(def USER_ID 1)
(def OBJECTIVE_ID 2)
(def QUESTION_ID 3)
(def GLOBAL_ID 5)

(defn requestify [params]
  {:params (dissoc params :end-date)})

(def date-time (utils/string->date-time "2015-01-03"))

(def test-objective {:title "My Objective"
                    :goals "To rock out, All day"
                    :description "I like cake"
                    :end-date date-time})

(fact "creates an objective from a request"
        (let [objective (request->objective (requestify test-objective)
                                            USER_ID)]
          (:created-by-id objective) => 1
          (:end-date objective) =not=> nil))

(fact "extracts comment data from a request"
      (let [comment-data (request->comment-data {:params {:comment "the comment"
                                                          :comment-on-uri "/some/uri"}}
                                                USER_ID)]
        comment-data => {:comment "the comment"
                         :comment-on-uri "/some/uri"
                         :created-by-id USER_ID}))

(fact "creates a question from a request"
      (let [question (request->question {:route-params {:id (str OBJECTIVE_ID)}
                                         :params {:question "the question"}}
                                         USER_ID)]
           question => {:question "the question"
                        :created-by-id USER_ID
                        :objective-id OBJECTIVE_ID}))

(fact "creates answer-info map from a request"
      (let [answer (request->answer-info {:route-params {:id (str OBJECTIVE_ID)
                                                         :q-id (str QUESTION_ID)} 
                                          :params {:answer "the answer"}} USER_ID)]
        answer => {:answer "the answer"
                   :question-id QUESTION_ID
                   :objective-id OBJECTIVE_ID
                   :created-by-id USER_ID}))

(fact "creates invited-writer-info map from a request"
      (let [writer (request->invitation-info {:route-params {:id (str OBJECTIVE_ID)} 
                                                  :params {:writer-name "Jenny" :reason "Just because"}} USER_ID)]
        writer => {:writer-name "Jenny"
                   :reason "Just because"
                   :objective-id OBJECTIVE_ID
                   :invited-by-id USER_ID}))

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
