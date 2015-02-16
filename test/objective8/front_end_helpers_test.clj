(ns objective8.front-end-helpers-test
  (:require [midje.sweet :refer :all]
            [objective8.front-end-helpers :refer :all]))


(def USER_ID 1)
(def OBJECTIVE_ID 2)

(fact "creates a comment from a request"
      (let [comment (request->comment {:params {:comment "the comment"
                                                :objective-id (str OBJECTIVE_ID)}}
                                        USER_ID)]
           comment => {:comment "the comment"
                       :objective-id OBJECTIVE_ID
                       :created-by-id USER_ID}))

(fact "creates a question from a request"
      (let [question (request->question {:route-params {:id (str OBJECTIVE_ID)}
                                         :params {:question "the question"}}
                                         USER_ID)]
           question => {:question "the question"
                        :created-by-id USER_ID
                        :objective-id OBJECTIVE_ID}))
