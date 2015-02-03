(ns d-cent.utils-test
  (:require [midje.sweet :refer :all]
            [d-cent.utils :refer :all]))


(fact "should convert a time-string into a pretty-date"
  (time-string->pretty-date "2015-12-01T00:00:00.000Z") => "01-12-2015")