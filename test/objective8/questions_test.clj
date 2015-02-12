(ns objective8.questions-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [objective8.storage.storage :as storage]
            [objective8.utils :as utils]
            [objective8.questions :refer :all]))

(def USER_ID 1)
(def OBJECTIVE_ID 2)

(fact "creates a question from a request"
      (against-background
        (friend/current-authentication) => {:username USER_ID})
      (let [question (request->question {:route-params {:id (str OBJECTIVE_ID)}
                                         :params {:question "the question"}})]
           question => {:question "the question"
                        :created-by-id USER_ID
                        :objective-id OBJECTIVE_ID}))
