(ns objective8.unit.comments-test
  (:require [midje.sweet :refer :all]
            [objective8.comments :as comments]
            [objective8.objectives :as objectives]
            [objective8.storage.storage :as storage]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)
(def GLOBAL_ID 345)

(def comment {:objective-id OBJECTIVE_ID
              :comment-on-id GLOBAL_ID})
