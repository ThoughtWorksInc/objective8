(ns objective8.comments-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [objective8.comments :as comments]))

(def USER_ID 1)

(fact "creates a comment from a request"
      (against-background
        (friend/current-authentication) => {:username USER_ID})
      (let [comment (comments/request->comment {:params {:comment "the comment"
                                                         :objective-id "123"}})]
           comment => {:comment "the comment"
                       :objective-id 123
                       :created-by-id USER_ID}))
