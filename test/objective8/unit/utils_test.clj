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

;; A reference link that has without a reference
(def INVALID_MARKDOWN "[id]: http://octodex.github.com/images/dojocat.jpg  \"The Dojocat\"")

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
             (markdown->hiccup INVALID_MARKDOWN) => '()))

(def SOME_INVALID_HICCUP [["p" nil ")"] "\n"  ["p" nil "("] nil])

(facts "about sanitising hiccup"
       (fact "removes non-sequential elements and nils"
             (sanitise-hiccup SOME_INVALID_HICCUP) 
             => [["p" nil ")"] ["p" nil "("]]))

(def URI "/objectives/765/dashboard/questions")
(def URI_WITH_QUERY "/objectives/765/dashboard/questions?answer-view=down-votes&selected=%2Fobjectives%2F765%2Fquestions%2F177")
(def QUERY "answer-view=down-votes&selected=%2Fobjectives%2F765%2Fquestions%2F177")

(def RING_REQUEST {:uri URI :query-string nil})
(def RING_REQUEST_WITH_QUERY {:uri URI :query-string QUERY})

(facts "about sanitising referrals"
       (fact "when a referral route is not safe, safen-url returns nil"
             (safen-url "/unsafe-route") => nil)

       (fact "when there is a query string referer-url returns the query string appended to the uri"
             (referer-url RING_REQUEST_WITH_QUERY) => URI_WITH_QUERY)

       (fact "when there is no query string referer-url returns just the uri"
            (referer-url RING_REQUEST) => URI))

