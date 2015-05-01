(ns objective8.unit.utils-test
  (:require [midje.sweet :refer :all]
            [clj-time.core :as time-core] 
            [objective8.utils :refer :all])) 


(def invalid-markdown "[id]: http://octodex.github.com/images/dojocat.jpg  \"The Dojocat\"")

(fact "should add 30 days to given time"
      (date-time->date-time-plus-30-days (time-core/date-time 2015 10 01 4 3 27 456)) => 
      (time-core/date-time 2015 10 31 4 3 27 456))

(fact "should return number of days until given time"
      (days-until (time-core/plus (time-core/now) (time-core/hours 36))) => 1)

(fact "generate-random-uuid returns a long random string"
      (let [uuid1 (generate-random-uuid)
            uuid2 (generate-random-uuid)]
        uuid1 =not=> uuid2 
        (class uuid1) => String
        (count uuid1) => 36))

(facts "about converting markdown to hiccup"
       (fact "correctly converts valid markdown"
             (markdown->hiccup "HEADLINE\n===\n\n- l1\n- l2\n")
             => '([:h1 "HEADLINE"] [:ul [:li "l1"] [:li "l2"]]))

       (fact "suppresses evil html script tag"
             (markdown->hiccup "<script>alert('evil')</script>")
             => '())

       (fact "suppresses evil html link"
             (markdown->hiccup "<a href='evil-url'>evil-link</a>")
             => '([:p "\n" "evil-link" "\n"]))
       
       (fact "filters out nil hiccup elements"
             (markdown->hiccup invalid-markdown) => '()))

(facts "about sanitising referrals"
       (fact "when a referral route is not safe, safen-url returns nil"
             (safen-url "/unsafe-route") => nil))
