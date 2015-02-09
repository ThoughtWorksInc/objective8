(ns objective8.comments-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [objective8.comments :as comments]))

(def OBJECTIVE_ID 234)
(def USER_ID 1)

(def test-comment {:comment "the comment"
                   :objective-id (str OBJECTIVE_ID)})

(fact "creates a comment from a request"
      (against-background
        (friend/current-authentication) => {:username USER_ID})
      (let [comment (comments/request->comment {:params test-comment})]
          (:created-by-id comment) => USER_ID))
