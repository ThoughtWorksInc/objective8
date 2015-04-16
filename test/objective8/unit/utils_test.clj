(ns objective8.unit.utils-test
  (:require [midje.sweet :refer :all]
            [clj-time.core :as time-core] 
            [objective8.utils :refer :all])) 

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
             (hiccup->html (markdown->hiccup "HEADLINE\n===\n\n- l1\n- l2\n"))
             => "<h1>HEADLINE</h1><ul><li>l1</li><li>l2</li></ul>")

       (fact "suppresses evil html script tag"
             (hiccup->html (markdown->hiccup "<script>alert('evil')</script>")) 
             =not=> (contains "<script>"))

       (fact "suppresses evil html link"
             (hiccup->html (markdown->hiccup "<a href='evil-url'>evil-link</a>")) 
             =not=> (contains "href")))

(facts "about sanitising referrals"
       (fact "when a referral route is not safe, safen-url returns nil"
             (safen-url "/unsafe-route") => nil))
